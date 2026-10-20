package ai.copilotdrive.app.platform

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.service.notification.NotificationListenerService
import ai.copilotdrive.core.*
import kotlinx.coroutines.flow.MutableStateFlow

/** Does not inspect, retain or forward notification content. */
class MediaAccessService : NotificationListenerService()

class AndroidMediaAdapter(private val context: Context) : MediaSourceAdapter {
    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(context, MediaAccessService::class.java)
    private val handler = Handler(Looper.getMainLooper())
    override val state = MutableStateFlow(MediaSnapshot(reason = "Acceso a sesiones pendiente"))
    val sessions = MutableStateFlow<List<MediaSessionChoice>>(emptyList())
    val artwork = MutableStateFlow<Bitmap?>(null)
    private var controllers = emptyList<MediaController>()
    private var selected: String? = null
    private var active: MediaController? = null
    private var observing = false
    private val listener = MediaSessionManager.OnActiveSessionsChangedListener { refresh() }
    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(playbackState: PlaybackState?) { refresh() }
        override fun onMetadataChanged(metadata: MediaMetadata?) { publish() }
        override fun onSessionDestroyed() { refresh() }
    }
    fun authorized() = if (Build.VERSION.SDK_INT >= 27) {
        context.getSystemService(NotificationManager::class.java).isNotificationListenerAccessGranted(component)
    } else {
        // Compat caches the previous package set when the setting becomes null.
        // API26 must observe revocation immediately, for this exact listener.
        Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            .orEmpty().split(':').any { ComponentName.unflattenFromString(it) == component }
    }
    private fun id(controller: MediaController) = "${controller.packageName}:${controller.sessionToken.hashCode()}"

    fun refresh() {
        if (!authorized()) { clear("Permite el acceso a notificaciones para consultar sesiones"); return }
        try {
            if (!observing) {
                manager.addOnActiveSessionsChangedListener(listener, component, handler)
                observing = true
            }
            controllers = manager.getActiveSessions(component).sortedBy { id(it) }
            sessions.value = controllers.map { MediaSessionChoice(id(it), it.packageName, it.playbackState?.state == PlaybackState.STATE_PLAYING) }
            val chosen = selectMediaSession(sessions.value, selected)
            if (selected != null && sessions.value.none { it.id == selected }) selected = null
            val next = controllers.firstOrNull { id(it) == chosen }
            if (active?.sessionToken != next?.sessionToken) {
                active?.unregisterCallback(callback)
                active = next
                active?.registerCallback(callback, handler)
            }
            publish()
        } catch (_: SecurityException) { clear("Acceso multimedia denegado o revocado") }
    }
    fun select(id: String) { selected = id; refresh() }

    private fun publish() {
        val controller = active
        if (controller == null) {
            artwork.value = null
            state.value = MediaSnapshot(reason = if (sessions.value.size > 1) "Varias sesiones: elige un reproductor en Ajustes" else "Sin sesión multimedia accesible")
            return
        }
        val metadata = controller.metadata
        artwork.value = metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val playback = controller.playbackState
        val actions = playback?.actions ?: 0L
        val controls = buildSet {
            if (actions and (PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PLAY_PAUSE) != 0L) add(MediaControl.PLAY)
            if (actions and (PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE) != 0L) add(MediaControl.PAUSE)
            if (actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L) add(MediaControl.NEXT)
            if (actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L) add(MediaControl.PREVIOUS)
        }
        state.value = MediaSnapshot(
            sourceApp = controller.packageName, title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST), album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM),
            artworkUri = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI),
            durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.takeIf { it > 0 },
            positionMs = playback?.position?.takeIf { it >= 0 },
            playbackStatus = when (playback?.state) { PlaybackState.STATE_PLAYING -> PlaybackStatus.PLAYING; PlaybackState.STATE_PAUSED -> PlaybackStatus.PAUSED; PlaybackState.STATE_STOPPED -> PlaybackStatus.STOPPED; else -> PlaybackStatus.UNAVAILABLE },
            supportedControls = controls, capturedAt = System.currentTimeMillis(),
        )
    }

    override suspend fun command(control: MediaControl): ToolResult {
        refresh() // Recheck revocation, session disappearance and supported controls before dispatch.
        val controller = active ?: return ToolResult.denied("media", "NO_SESSION", state.value.reason ?: "Sin sesión")
        if (control !in state.value.supportedControls) return ToolResult.denied("media", "UNSUPPORTED", "Control no disponible.")
        try {
            when (control) {
                MediaControl.PLAY -> controller.transportControls.play()
                MediaControl.PAUSE -> controller.transportControls.pause()
                MediaControl.NEXT -> controller.transportControls.skipToNext()
                MediaControl.PREVIOUS -> controller.transportControls.skipToPrevious()
                MediaControl.SEEK -> return ToolResult.denied("media", "UNSUPPORTED", "Desplazamiento no implementado.")
            }
        } catch (_: SecurityException) {
            clear("Permiso multimedia revocado")
            return ToolResult.denied("media", "PERMISSION_REQUIRED", "Permiso multimedia revocado.")
        }
        // No optimistic state: dashboard waits for MediaController callback.
        return ToolResult("media.${control.name.lowercase()}", ExecutionStatus.REQUESTED, "Control solicitado al reproductor. Estado pendiente de confirmación.")
    }

    private fun clear(reason: String) {
        active?.unregisterCallback(callback); active = null; controllers = emptyList(); sessions.value = emptyList(); selected = null
        if (observing) { manager.removeOnActiveSessionsChangedListener(listener); observing = false }
        state.value = MediaSnapshot(reason = reason)
        artwork.value = null
    }
    fun close() { clear("Sesión cerrada") }
}
