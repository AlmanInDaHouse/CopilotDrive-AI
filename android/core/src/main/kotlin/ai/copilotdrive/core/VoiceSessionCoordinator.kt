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
data class VoiceSessionState(val state: VoiceState = VoiceState.IDLE, val message: String = "Listo para ayudarte", val elapsedMs: Long = 0, val errorSource: String? = null)

class VoiceSessionCoordinator(
    private val scope: CoroutineScope,
    private val stt: SpeechToTextProvider,
    private val tts: TextToSpeechProvider,
    private val assistant: AssistantOrchestrator,
    private val focus: AudioFocusCoordinator,
    private val clock: Clock,
    private val onTiming: (VoiceMetric, Long) -> Unit = { _, _ -> },
    private val onFailure: (String, String) -> Unit = { _, _ -> },
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
                    val recognitionStart = System.nanoTime()
                    withTimeout(15_000) { stt.recognize { transition(VoiceState.TRANSCRIBING, "Procesando voz…") } }
                        .also { onTiming(VoiceMetric.RECOGNITION, ((System.nanoTime() - recognitionStart) / 1_000_000).coerceAtLeast(0)) }
                }
                check(text.isNotBlank()) { "No se ha detectado voz" }
                transition(VoiceState.UNDERSTANDING, "Interpretando petición…")
                val request = AssistantRequest("voice-$started-$token", "foreground", text, if (demo == null) InputMode.VOICE else InputMode.DEMO, clock.now())
                transition(VoiceState.EXECUTING, "Consultando…")
                val orchestratorStart = System.nanoTime()
                val result = assistant.answer(request)
                onTiming(VoiceMetric.ORCHESTRATOR, ((System.nanoTime() - orchestratorStart) / 1_000_000).coerceAtLeast(0))
                transition(VoiceState.SPEAKING, result.message)
                val replyErrorSource = if (result.toolId == "assistant" && result.errorCode != null) "AI" else null
                if (token == generation) mutable.value = mutable.value.copy(errorSource = replyErrorSource)
                val ttsStart = System.nanoTime()
                withTimeout(20_000) { tts.speak(result.message) }
                onTiming(VoiceMetric.TTS_CALLBACK, ((System.nanoTime() - ttsStart) / 1_000_000).coerceAtLeast(0))
                transition(VoiceState.IDLE)
                if (token == generation) mutable.value = mutable.value.copy(errorSource = replyErrorSource)
            } catch (_: TimeoutCancellationException) {
                val timedOutWhileSpeaking = mutable.value.state == VoiceState.SPEAKING
                onFailure(if (timedOutWhileSpeaking) "TTS" else "STT", if (timedOutWhileSpeaking) "TTS_TIMEOUT" else "STT_TIMEOUT")
                transition(VoiceState.ERROR, if (timedOutWhileSpeaking) mutable.value.message + " · Voz no disponible (tiempo agotado)." else "Tiempo de espera agotado. Puedes volver a intentarlo.")
                if (token == generation) mutable.value = mutable.value.copy(errorSource = if (timedOutWhileSpeaking) "TTS" else "STT")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SecurityException) {
                onFailure("STT", "PERMISSION_OR_CONSENT")
                transition(VoiceState.PERMISSION_REQUIRED, "Falta permiso o consentimiento para reconocimiento.")
            } catch (failure: Exception) {
                val speaking = mutable.value.state == VoiceState.SPEAKING
                val stage = if (speaking) "TTS" else if (mutable.value.state == VoiceState.EXECUTING) "ORCHESTRATOR" else "STT"
                val code = failure.message?.takeIf { it in setOf("STT_NO_MATCH", "STT_SILENCE", "STT_NETWORK_TIMEOUT", "STT_BUSY", "STT_INTERRUPTED") }
                    ?: "${stage}_ERROR"
                onFailure(stage, code)
                val message = if (speaking) mutable.value.message + " · Voz no disponible." else "Voz no disponible. Revisa motor, permisos y conexión."
                transition(VoiceState.ERROR, message)
                mutable.value = mutable.value.copy(errorSource = if (speaking) "TTS" else "STT")
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
        onFailure("VOICE", "CANCELLED")
        mutable.value = VoiceSessionState(VoiceState.CANCELLED, "Micrófono apagado · Cancelado")
    }
}
