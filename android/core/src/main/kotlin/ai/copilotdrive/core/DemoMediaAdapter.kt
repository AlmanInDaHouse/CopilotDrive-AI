package ai.copilotdrive.core

import kotlinx.coroutines.flow.MutableStateFlow

/** Product demo source. It changes metadata only and never plays audio. */
class DemoMediaAdapter(private val clock: Clock) : MediaSourceAdapter {
    override val state = MutableStateFlow(
        MediaSnapshot(
            sourceApp = "Simulador de audio",
            title = "Midnight Drive",
            artist = "Biblioteca de demostración",
            durationMs = 210_000,
            positionMs = 42_000,
            playbackStatus = PlaybackStatus.PAUSED,
            supportedControls = setOf(MediaControl.PLAY, MediaControl.PAUSE, MediaControl.NEXT, MediaControl.PREVIOUS),
            capturedAt = clock.now(),
            simulated = true,
        ),
    )

    override suspend fun command(control: MediaControl): ToolResult {
        if (control !in state.value.supportedControls) {
            return ToolResult.denied("media", "UNSUPPORTED", "Control no disponible.")
        }
        state.value = state.value.copy(
            title = when (control) {
                MediaControl.NEXT -> "Coastal Road"
                MediaControl.PREVIOUS -> "Midnight Drive"
                else -> state.value.title
            },
            playbackStatus = when (control) {
                MediaControl.PLAY -> PlaybackStatus.PLAYING
                MediaControl.PAUSE -> PlaybackStatus.PAUSED
                else -> state.value.playbackStatus
            },
            capturedAt = clock.now(),
        )
        return ToolResult(
            "media.${control.name.lowercase()}",
            ExecutionStatus.SUCCESS,
            "Simulación. Reproductor: ${state.value.playbackStatus.name.lowercase()}.",
        )
    }
}
