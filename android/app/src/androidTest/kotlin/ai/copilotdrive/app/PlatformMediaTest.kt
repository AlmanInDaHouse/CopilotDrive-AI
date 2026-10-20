package ai.copilotdrive.app

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
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
        fun permission(allow: Boolean) {
            instrumentation.uiAutomation.executeShellCommand("cmd notification ${if (allow) "allow_listener" else "disallow_listener"} $component")
                .use { descriptor -> android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).readBytes() }
        }
        val adapter = AndroidMediaAdapter(context)
        val session = MediaSession(context, "CopilotDrive instrumentation fixture")
        val pauseRequested = AtomicBoolean(false)
        fun playback(state: Int) = PlaybackState.Builder().setState(state, 1000, 1f)
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE).build()
        try {
            permission(true)
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
            withContext(Dispatchers.Main) { adapter.refresh() }
            assertNull(adapter.state.value.title)
            assertTrue(adapter.state.value.supportedControls.isEmpty())
        } finally {
            withContext(Dispatchers.Main) { adapter.close() }
            session.release()
            permission(false)
        }
    }
}
