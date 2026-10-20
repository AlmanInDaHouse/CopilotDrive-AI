package ai.copilotdrive.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AcousticProbeStatus { PENDING, SPEAKING, COMPLETED, FAILED, CANCELLED }
data class AcousticProbeState(val attempt: Long = 0, val status: AcousticProbeStatus = AcousticProbeStatus.PENDING,
    val human: HumanOutcome = HumanOutcome.PENDING)

/** UI observations remain session-only. A build-bound acta is required for physical qualification. */
class AcousticProbe(private val scope: CoroutineScope, private val tts: TextToSpeechProvider,
    private val focus: AudioFocusCoordinator) {
    companion object { const val PHRASE = "Prueba de voz en español. Jarvis está listo para ayudarte." }
    private val mutable = MutableStateFlow(AcousticProbeState())
    val state = mutable.asStateFlow()
    private var job: Job? = null

    fun start() {
        cancel()
        val attempt = mutable.value.attempt + 1
        mutable.value = AcousticProbeState(attempt, AcousticProbeStatus.SPEAKING)
        job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                check(focus.acquire())
                withTimeout(20_000) { tts.speak(PHRASE) }
                if (mutable.value.attempt == attempt) mutable.value = mutable.value.copy(status = AcousticProbeStatus.COMPLETED)
            } catch (_: TimeoutCancellationException) {
                if (mutable.value.attempt == attempt) mutable.value = mutable.value.copy(status = AcousticProbeStatus.FAILED)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (mutable.value.attempt == attempt) mutable.value = mutable.value.copy(status = AcousticProbeStatus.FAILED)
            } finally {
                if (mutable.value.attempt == attempt) release()
            }
        }.also { it.start() }
    }
    fun confirm(attempt: Long, heard: Boolean) {
        if (attempt != mutable.value.attempt || mutable.value.status != AcousticProbeStatus.COMPLETED ||
            mutable.value.human != HumanOutcome.PENDING) return
        mutable.value = mutable.value.copy(human = if (heard) HumanOutcome.CONFIRMED else HumanOutcome.NOT_OBSERVED)
    }
    private fun release() { runCatching { tts.stop() }; runCatching { focus.release() } }
    fun cancel() {
        // Increment before cancelling: late cleanup must never stop a replacement utterance.
        if (job != null) {
            mutable.value = AcousticProbeState(mutable.value.attempt + 1, AcousticProbeStatus.CANCELLED)
            job?.cancel(); job = null; release()
        }
    }
}
