package ai.copilotdrive.testing

import ai.copilotdrive.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class VoiceReliabilityTest {
    @Test fun changingSourceModeClearsOldDemoAnswerAndCancelsAudio() = runTest {
        val h = TestHarness()
        val v = voice(this, h)
        v.demo("gasolina"); advanceUntilIdle()
        assertTrue(v.state.value.message.contains("42 %"))
        v.clearForModeChange(); advanceUntilIdle()
        assertEquals(VoiceState.IDLE, v.state.value.state)
        assertFalse(v.state.value.message.contains("42"))
        assertFalse(h.focus.held)
        assertEquals(0, h.stt.calls)
    }
    private fun voice(scope: CoroutineScope, h: TestHarness, stt: SpeechToTextProvider = h.stt,
        tts: TextToSpeechProvider = h.tts, permission: () -> Boolean = { true }) =
        VoiceSessionCoordinator(scope, stt, tts, h.assistant, h.focus, h.clock, hasMicrophonePermission = permission)

    @Test fun cancellationWhileSpeakingPreservesAnswerAndNewRequestRecovers() = runTest {
        val h = TestHarness(); h.tts.delayMs = 10_000
        val v = voice(this, h)
        v.listen(); runCurrent()
        assertEquals(VoiceState.SPEAKING, v.state.value.state)
        v.cancel(); advanceUntilIdle()
        assertTrue(v.state.value.message.contains("42 %")); assertFalse(h.focus.held)
        h.tts.delayMs = 0
        v.listen(); advanceUntilIdle()
        assertEquals(VoiceState.IDLE, v.state.value.state); assertEquals(2, h.stt.calls)
    }
    @Test fun ttsPermissionFailureNeverErasesAnswer() = runTest {
        val h = TestHarness(); h.tts.failure = SecurityException("private")
        val v = voice(this, h); v.listen(); advanceUntilIdle()
        assertTrue(v.state.value.message.contains("42 %")); assertEquals("TTS", v.state.value.errorSource)
        assertFalse(v.state.value.message.contains("private")); assertFalse(h.focus.held)
    }
    @Test fun cleanupExceptionsCannotSkipRemainingResourcesOrBlockRecovery() = runTest {
        val h = TestHarness(); var stops = 0
        val broken = object : SpeechToTextProvider {
            override suspend fun recognize(onTranscribing: () -> Unit): String { delay(5_000); return "gasolina" }
            override fun cancel() { throw IllegalStateException("private") }
        }
        val tts = object : TextToSpeechProvider {
            override suspend fun speak(text: String) = Unit
            override fun stop() { stops++ }
        }
        val v = voice(this, h, broken, tts); v.listen(); runCurrent(); v.cancel(); advanceUntilIdle()
        assertFalse(h.focus.held); assertTrue(stops >= 2); assertEquals(VoiceState.CANCELLED, v.state.value.state)
        v.demo("gasolina"); advanceUntilIdle(); assertEquals(VoiceState.IDLE, v.state.value.state)
    }
    @Test fun focusLossCancelAndRecoveryNeverAutoRestartRecognition() = runTest {
        val h = TestHarness(); h.stt.delayMs = 10_000
        val v = voice(this, h); v.listen(); runCurrent(); v.cancel(); advanceUntilIdle()
        assertEquals(1, h.stt.calls); assertFalse(h.focus.held)
        h.focus.granted = false; v.listen(); advanceUntilIdle()
        assertEquals("AUDIO_FOCUS", v.state.value.errorSource); assertEquals(1, h.stt.calls)
        h.focus.granted = true; h.stt.delayMs = 0; v.listen(); advanceUntilIdle()
        assertEquals(VoiceState.IDLE, v.state.value.state)
    }
    @Test fun eachRecognizerFailureAndEmptyResultRecoverOnNextExplicitRequest() = runTest {
        for (failure in listOf(IllegalStateException("engine absent"), IllegalStateException("STT_SILENCE"),
            IllegalStateException("STT_NO_MATCH"), IllegalStateException("STT_BUSY"), SecurityException("revoked"))) {
            val h = TestHarness(); val v = voice(this, h); h.stt.failure = failure
            v.listen(); advanceUntilIdle(); assertFalse(h.focus.held); assertTrue(h.tts.spoken.isEmpty())
            h.stt.failure = null; h.stt.text = ""; v.listen(); advanceUntilIdle()
            assertEquals(VoiceState.ERROR, v.state.value.state)
            h.stt.text = "gasolina"; v.listen(); advanceUntilIdle(); assertEquals(VoiceState.IDLE, v.state.value.state)
        }
    }
    @Test fun revokedPermissionBlocksNextSessionAndFastCancelCannotOverwriteReplacement() = runTest {
        val h = TestHarness(); var permitted = true
        val v = voice(this, h, permission = { permitted }); h.stt.delayMs = 10_000
        v.listen(); runCurrent(); v.cancel(); h.stt.delayMs = 0; v.listen(); advanceUntilIdle()
        assertEquals(VoiceState.IDLE, v.state.value.state); assertTrue(v.state.value.message.contains("42 %"))
        permitted = false; v.listen(); advanceUntilIdle()
        assertEquals(VoiceState.PERMISSION_REQUIRED, v.state.value.state); assertEquals(2, h.stt.calls)
    }
}
