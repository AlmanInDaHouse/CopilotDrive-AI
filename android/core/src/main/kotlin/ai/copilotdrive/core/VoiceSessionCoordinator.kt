package ai.copilotdrive.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

enum class VoiceState { IDLE, LISTENING, TRANSCRIBING, UNDERSTANDING, EXECUTING, SPEAKING, CANCELLED, PERMISSION_REQUIRED, ERROR }
data class VoiceSessionState(val state: VoiceState = VoiceState.IDLE, val message: String = "Listo para ayudarte", val elapsedMs: Long = 0)

class VoiceSessionCoordinator(
    private val scope: CoroutineScope,
    private val stt: SpeechToTextProvider,
    private val tts: TextToSpeechProvider,
    private val assistant: AssistantOrchestrator,
    private val focus: AudioFocusCoordinator,
    private val clock: Clock,
    private val hasMicrophonePermission: () -> Boolean,
) {
    private val mutable = MutableStateFlow(VoiceSessionState())
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var generation = 0

    fun listen() = start(null)
    fun demo(text: String) = start(text)

    private fun start(demo: String?) {
        if (job?.isActive == true) return
        if (demo == null && !hasMicrophonePermission()) {
            mutable.value = VoiceSessionState(VoiceState.PERMISSION_REQUIRED, "Permite el micrófono para hablar.")
            return
        }
        val token = ++generation
        val started = clock.now()
        fun transition(next: VoiceState, message: String = mutable.value.message) {
            if (token == generation) mutable.value = VoiceSessionState(next, message, (clock.now() - started).coerceAtLeast(0))
        }
        job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                check(focus.acquire()) { "Audio ocupado" }
                tts.stop()
                val text = demo ?: run {
                    transition(VoiceState.LISTENING, "Micrófono activo · Te escucho")
                    withTimeout(15_000) { stt.recognize { transition(VoiceState.TRANSCRIBING, "Procesando voz…") } }
                }
                check(text.isNotBlank()) { "No se ha detectado voz" }
                transition(VoiceState.UNDERSTANDING, "Interpretando petición…")
                val request = AssistantRequest("voice-$started-$token", "foreground", text, if (demo == null) InputMode.VOICE else InputMode.DEMO, clock.now())
                transition(VoiceState.EXECUTING, "Consultando…")
                val result = assistant.answer(request)
                transition(VoiceState.SPEAKING, result.message)
                withTimeout(20_000) { tts.speak(result.message) }
                transition(VoiceState.IDLE)
            } catch (_: TimeoutCancellationException) {
                transition(VoiceState.ERROR, "Tiempo de espera agotado. Puedes volver a intentarlo.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SecurityException) {
                transition(VoiceState.PERMISSION_REQUIRED, "Falta permiso o consentimiento para reconocimiento.")
            } catch (_: Exception) {
                val message = if (mutable.value.state == VoiceState.SPEAKING) mutable.value.message + " · Voz no disponible." else "Voz no disponible. Revisa motor, permisos y conexión."
                transition(VoiceState.ERROR, message)
            } finally {
                if (token == generation) {
                    stt.cancel()
                    tts.stop()
                    focus.release()
                }
            }
        }.also { it.start() }
    }

    fun cancel() {
        generation++
        job?.cancel()
        stt.cancel()
        tts.stop()
        focus.release()
        assistant.cancelPending()
        mutable.value = VoiceSessionState(VoiceState.CANCELLED, "Micrófono apagado · Cancelado")
    }
}
