package ai.copilotdrive.testing

import ai.copilotdrive.core.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PhysicalVoicePilotTest {
    @Test fun localJarvisDoesNotInvokeUnavailableRemoteProviderTestA() = runTest {
        val h = TestHarness()
        h.ai.response = AIResponse.Unavailable(AIError("NETWORK_DISABLED", "Red fake desactivada"))
        val result = h.ask("gasolina")
        assertEquals(ExecutionStatus.SUCCESS, result.executionStatus)
        assertTrue(result.message.contains("Simulación."))
        assertEquals(0, h.ai.calls)
        // Test A only: no claim about actual Android connectivity or acoustic playback.
    }
    @Test fun callbackDoesNotConfirmAudibilityAndRetryInvalidatesOldConfirmation() = runTest {
        val h = TestHarness(); val p = AcousticProbe(this, h.tts, h.focus)
        p.start(); advanceUntilIdle()
        val first = p.state.value.attempt
        assertEquals(AcousticProbeStatus.COMPLETED, p.state.value.status)
        assertEquals(HumanOutcome.PENDING, p.state.value.human)
        p.confirm(first, false); assertEquals(HumanOutcome.NOT_OBSERVED, p.state.value.human)
        p.confirm(first, true); assertEquals(HumanOutcome.NOT_OBSERVED, p.state.value.human)
        p.start(); advanceUntilIdle(); p.confirm(first, true)
        assertEquals(HumanOutcome.PENDING, p.state.value.human)
        p.confirm(p.state.value.attempt, true); assertEquals(HumanOutcome.CONFIRMED, p.state.value.human)
        assertFalse(h.focus.held)
    }
    @Test fun timeoutCancellationAndFailureRecoverWithoutAcceptingHumanConfirmation() = runTest {
        val h = TestHarness(); val p = AcousticProbe(this, h.tts, h.focus)
        h.tts.delayMs = 30_000; p.start(); advanceUntilIdle()
        assertEquals(AcousticProbeStatus.FAILED, p.state.value.status); assertFalse(h.focus.held)
        p.confirm(p.state.value.attempt, true); assertEquals(HumanOutcome.PENDING, p.state.value.human)
        p.start(); runCurrent(); p.cancel(); advanceUntilIdle()
        assertEquals(AcousticProbeStatus.CANCELLED, p.state.value.status); assertFalse(h.focus.held)
        h.tts.delayMs = 0; h.tts.failure = SecurityException("secret")
        p.start(); advanceUntilIdle(); assertEquals(AcousticProbeStatus.FAILED, p.state.value.status)
        h.tts.failure = null; p.start(); advanceUntilIdle()
        assertEquals(AcousticProbeStatus.COMPLETED, p.state.value.status)
    }
    @Test fun cancelledOldProbeCannotStopReplacementAndBackgroundCannotKeepConfirmation() = runTest {
        val h = TestHarness(); val p = AcousticProbe(this, h.tts, h.focus)
        h.tts.delayMs = 1000; p.start(); runCurrent(); p.cancel(); p.start(); runCurrent()
        assertTrue(h.focus.held); advanceUntilIdle()
        assertEquals(AcousticProbeStatus.COMPLETED, p.state.value.status)
        p.cancel(); p.confirm(p.state.value.attempt, true)
        assertEquals(HumanOutcome.PENDING, p.state.value.human); assertFalse(h.focus.held)
    }
    @Test fun voiceJourneyHasStagesWithoutTranscriptOrToolArguments() = runTest {
        val h = TestHarness(); val stages = mutableListOf<VoiceJourneyStage>()
        val v = VoiceSessionCoordinator(this, h.stt, h.tts, h.assistant, h.focus, h.clock,
            hasMicrophonePermission = { true }, onJourney = stages::add)
        v.listen(); advanceUntilIdle()
        assertEquals(listOf(VoiceJourneyStage.REQUEST_STARTED, VoiceJourneyStage.STT_LISTENING,
            VoiceJourneyStage.STT_TRANSCRIBING, VoiceJourneyStage.TRANSCRIPT_OBTAINED,
            VoiceJourneyStage.ORCHESTRATOR_PROCESSING, VoiceJourneyStage.TEXT_RESPONSE,
            VoiceJourneyStage.TTS_STARTED, VoiceJourneyStage.TTS_COMPLETED), stages)
        h.tts.failure = IllegalStateException("secret"); v.listen(); advanceUntilIdle()
        assertEquals(VoiceJourneyStage.FAILED, stages.last()); assertTrue(v.state.value.message.contains("42 %"))
    }
    @Test fun policyEventsDoNotClaimAnObservedExternalEffect() = runTest {
        val h = TestHarness(); val stages = mutableListOf<VoiceJourneyStage>()
        val tools = ToolRegistry(PolicyEngine(CapabilityRegistry(h.permissions)), h.context, h.media, h.navigation, h.contacts, onJourney = stages::add)
        tools.execute(ToolRequest("phone.open_dialer", mapOf("number" to "123456789")))
        assertEquals(listOf(VoiceJourneyStage.TOOL_REQUESTED, VoiceJourneyStage.TOOL_REJECTED), stages)
        stages.clear(); tools.execute(ToolRequest("vehicle.get_fuel"))
        assertEquals(listOf(VoiceJourneyStage.TOOL_REQUESTED, VoiceJourneyStage.TOOL_AUTHORIZED), stages)
    }
}
