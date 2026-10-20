package ai.copilotdrive.app

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import ai.copilotdrive.app.platform.AndroidMediaAdapter
import ai.copilotdrive.app.platform.MediaAccessService
import ai.copilotdrive.core.ExecutionStatus
import ai.copilotdrive.core.MediaControl
import ai.copilotdrive.core.PlaybackStatus
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class PlatformMediaTest {
    @Test fun authorizedSessionPauseAndRevocation() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val component = ComponentName(context, MediaAccessService::class.java).flattenToString()
        fun shell(command: String): String = instrumentation.uiAutomation.executeShellCommand(command)
            .use { descriptor -> android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).readBytes().toString(Charsets.UTF_8).trim() }
        // UiAutomation uses Runtime.exec, not a shell: quotes would become literal data.
        fun listenerArgument(value: String): String {
            require(value.matches(Regex("[A-Za-z0-9_.$/:]+"))) { "UNEXPECTED_LISTENER_FIXTURE_STATE" }
            return value
        }
        val originalListeners = shell("settings get secure enabled_notification_listeners")
        val originalComponents = originalListeners.takeUnless { it == "null" }.orEmpty().split(':').filter { it.isNotBlank() }
        val originallyAllowed = originalComponents.any { ComponentName.unflattenFromString(it) == ComponentName(context, MediaAccessService::class.java) }
        fun permission(allow: Boolean) {
            if (Build.VERSION.SDK_INT == 26) {
                // Android 8's notification service has no shell command implementation.
                // This setting drives its listener ContentObserver. Disposable test AVD only.
                val listeners = originalComponents.filterNot { ComponentName.unflattenFromString(it) == ComponentName(context, MediaAccessService::class.java) }.toMutableList()
                if (allow) listeners += component
                if (listeners.isEmpty()) shell("settings delete secure enabled_notification_listeners")
                else shell("settings put secure enabled_notification_listeners ${listenerArgument(listeners.joinToString(":"))}")
                check(shell("settings get secure enabled_notification_listeners") == listeners.joinToString(":").ifEmpty { "null" }) { "LISTENER_FIXTURE_SETTING_FAILED" }
            } else {
                val output = shell("cmd notification ${if (allow) "allow_listener" else "disallow_listener"} $component")
                check(!output.contains("Unknown command") && !output.contains("No shell command") && !output.contains("Exception")) { "LISTENER_FIXTURE_GRANT_FAILED" }
            }
        }
        val adapter = AndroidMediaAdapter(context)
        val session = MediaSession(context, "CopilotDrive instrumentation fixture")
        val pauseRequested = AtomicBoolean(false)
        fun playback(state: Int) = PlaybackState.Builder().setState(state, 1000, 1f)
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE).build()
        try {
            permission(true)
            withTimeout(5_000) { while (!adapter.authorized()) delay(50) }
            session.setCallback(object : MediaSession.Callback() {
                override fun onPause() { pauseRequested.set(true); session.setPlaybackState(playback(PlaybackState.STATE_PAUSED)) }
            }, Handler(Looper.getMainLooper()))
            session.setMetadata(MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, "Instrumented track").build())
            session.setPlaybackState(playback(PlaybackState.STATE_PLAYING))
            session.isActive = true
            withTimeout(10_000) {
                while (adapter.state.value.title != "Instrumented track") {
                    withContext(Dispatchers.Main) { adapter.refresh() }
                    delay(100)
                }
            }
            assertFalse(MediaControl.NEXT in adapter.state.value.supportedControls)
            val result = withContext(Dispatchers.Main) { adapter.command(MediaControl.PAUSE) }
            assertEquals(ExecutionStatus.REQUESTED, result.executionStatus)
            withTimeout(5_000) { while (adapter.state.value.playbackStatus != PlaybackStatus.PAUSED) delay(50) }
            assertTrue(pauseRequested.get())
            permission(false)
            assertFalse(adapter.authorized())
            withContext(Dispatchers.Main) { adapter.refresh() }
            assertNull(adapter.state.value.title)
            assertTrue(adapter.state.value.supportedControls.isEmpty())
        } finally {
            withContext(Dispatchers.Main) { adapter.close() }
            session.release()
            if (Build.VERSION.SDK_INT == 26) {
                if (originalListeners == "null" || originalListeners.isBlank()) shell("settings delete secure enabled_notification_listeners")
                else shell("settings put secure enabled_notification_listeners ${listenerArgument(originalListeners)}")
                check(shell("settings get secure enabled_notification_listeners") == originalListeners.ifBlank { "null" }) { "LISTENER_FIXTURE_RESTORE_FAILED" }
            } else permission(originallyAllowed)
            check(adapter.authorized() == originallyAllowed) { "LISTENER_FIXTURE_RESTORE_FAILED" }
        }
    }
}
