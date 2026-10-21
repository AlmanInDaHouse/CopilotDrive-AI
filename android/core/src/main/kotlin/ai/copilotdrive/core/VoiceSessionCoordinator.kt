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
    private val onJourney: (VoiceJourneyStage) -> Unit = {},
    private val hasMicrophonePermission: () -> Boolean,
) {
    private val mutable = MutableStateFlow(VoiceSessionState())
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var generation = 0
    private var writtenAnswer: String? = null

    // A broken provider's cleanup must not retain the microphone or audio focus.
    private fun releaseAudio() {
        runCatching { stt.cancel() }
        runCatching { tts.stop() }
        runCatching { focus.release() }
    }

    fun listen() = start(null)
    fun demo(text: String) = start(text)

    /** A source-mode change must not carry a previous demo answer into REAL. */
    fun clearForModeChange() {
        cancel()
        writtenAnswer = null
        mutable.value = VoiceSessionState()
    }

    private fun start(demo: String?) {
        if (job?.isActive == true) return
        onJourney(VoiceJourneyStage.REQUEST_STARTED)
        if (demo == null && !hasMicrophonePermission()) {
            onJourney(VoiceJourneyStage.PERMISSION_REQUIRED)
            mutable.value = VoiceSessionState(VoiceState.PERMISSION_REQUIRED, "Permite el micrófono para hablar.")
            return
        }
        val token = ++generation
        writtenAnswer = null
        val started = clock.now()
        val startedNs = System.nanoTime()
        fun transition(next: VoiceState, message: String = mutable.value.message) {
            if (token == generation) mutable.value = VoiceSessionState(next, message, ((System.nanoTime() - startedNs) / 1_000_000).coerceAtLeast(0))
        }
        job = scope.launch(start = CoroutineStart.LAZY) {
            var operation = "AUDIO_FOCUS"
            try {
                check(focus.acquire()) { "Audio ocupado" }
                tts.stop()
                operation = "STT"
                val text = demo ?: run {
                    transition(VoiceState.LISTENING, "Micrófono activo · Te escucho")
                    onJourney(VoiceJourneyStage.STT_LISTENING)
                    val recognitionStart = System.nanoTime()
                    withTimeout(15_000) { stt.recognize { if (token == generation) {
                        transition(VoiceState.TRANSCRIBING, "Procesando voz…")
                        onJourney(VoiceJourneyStage.STT_TRANSCRIBING)
                    } } }
                        .also { onTiming(VoiceMetric.RECOGNITION, ((System.nanoTime() - recognitionStart) / 1_000_000).coerceAtLeast(0)) }
                }
                if (token != generation) return@launch
                check(text.isNotBlank()) { "No se ha detectado voz" }
                if (demo == null) onJourney(VoiceJourneyStage.TRANSCRIPT_OBTAINED)
                onJourney(VoiceJourneyStage.ORCHESTRATOR_PROCESSING)
                transition(VoiceState.UNDERSTANDING, "Interpretando petición…")
                val request = AssistantRequest("voice-$started-$token", "foreground", text, if (demo == null) InputMode.VOICE else InputMode.DEMO, clock.now())
                transition(VoiceState.EXECUTING, "Consultando…")
                operation = "ORCHESTRATOR"
                val orchestratorStart = System.nanoTime()
                val result = assistant.answer(request)
                if (token != generation) return@launch
                writtenAnswer = result.message
                onJourney(VoiceJourneyStage.TEXT_RESPONSE)
                onTiming(VoiceMetric.ORCHESTRATOR, ((System.nanoTime() - orchestratorStart) / 1_000_000).coerceAtLeast(0))
                transition(VoiceState.SPEAKING, result.message)
                operation = "TTS"
                onJourney(VoiceJourneyStage.TTS_STARTED)
                val replyErrorSource = if (result.toolId == "assistant" && result.errorCode != null) "AI" else null
                if (token == generation) mutable.value = mutable.value.copy(errorSource = replyErrorSource)
                val ttsStart = System.nanoTime()
                withTimeout(20_000) { tts.speak(result.message) }
                if (token == generation) onJourney(VoiceJourneyStage.TTS_COMPLETED)
                onTiming(VoiceMetric.TTS_CALLBACK, ((System.nanoTime() - ttsStart) / 1_000_000).coerceAtLeast(0))
                transition(VoiceState.IDLE)
                if (token == generation) mutable.value = mutable.value.copy(errorSource = replyErrorSource)
            } catch (_: TimeoutCancellationException) {
                if (token != generation) return@launch
                val timedOutWhileSpeaking = operation == "TTS"
                onJourney(VoiceJourneyStage.FAILED)
                onFailure(operation, "${operation}_TIMEOUT")
                transition(VoiceState.ERROR, if (timedOutWhileSpeaking) mutable.value.message + " · Voz no disponible (tiempo agotado)." else "Tiempo de espera agotado. Puedes volver a intentarlo.")
                if (token == generation) mutable.value = mutable.value.copy(errorSource = operation)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SecurityException) {
                if (token == generation) {
                    onJourney(VoiceJourneyStage.PERMISSION_REQUIRED)
                    val stage = operation
                    onFailure(stage, "PERMISSION_OR_CONSENT")
                    transition(VoiceState.PERMISSION_REQUIRED, writtenAnswer?.plus(" · Voz sin permiso.")
                        ?: "Falta permiso o consentimiento para reconocimiento.")
                    mutable.value = mutable.value.copy(errorSource = stage)
                }
            } catch (failure: Exception) {
                if (token != generation) return@launch
                onJourney(VoiceJourneyStage.FAILED)
                val speaking = mutable.value.state == VoiceState.SPEAKING
                val stage = operation
                val code = failure.message?.takeIf { it in setOf("STT_NO_MATCH", "STT_SILENCE", "STT_NETWORK_TIMEOUT", "STT_BUSY", "STT_INTERRUPTED") }
                    ?: "${stage}_ERROR"
                onFailure(stage, code)
                val message = if (speaking) mutable.value.message + " · Voz no disponible." else "Voz no disponible. Revisa motor, permisos y conexión."
                transition(VoiceState.ERROR, message)
                if (token == generation) mutable.value = mutable.value.copy(errorSource = stage)
            } finally {
                if (token == generation) {
                    releaseAudio()
                }
            }
        }.also { it.start() }
    }

    fun cancel() {
        generation++
        job?.cancel()
        releaseAudio()
        assistant.cancelPending()
        onFailure("VOICE", "CANCELLED")
        onJourney(VoiceJourneyStage.CANCELLED)
        mutable.value = VoiceSessionState(VoiceState.CANCELLED,
            writtenAnswer?.plus(" · Micrófono apagado · Cancelado") ?: "Micrófono apagado · Cancelado")
    }
}
