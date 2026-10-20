package ai.copilotdrive.testing

import ai.copilotdrive.core.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class StabilityScenarioTest {
    @Test fun repeatedLocalVoiceFailureCancellationAndMediaRecovery() = runTest {
        val h = TestHarness()
        val voice = VoiceSessionCoordinator(this, h.stt, h.tts, h.assistant, h.focus, h.clock, hasMicrophonePermission = { true })
        repeat(100) { iteration ->
            h.tts.failure = IOException("fixture")
            voice.listen(); advanceUntilIdle()
            assertTrue(voice.state.value.message.contains("42 %"))
            assertFalse(h.focus.held)
            h.tts.failure = null
            voice.demo("gasolina"); advanceUntilIdle()
            assertEquals(VoiceState.IDLE, voice.state.value.state)
            h.stt.delayMs = 10_000
            voice.listen(); runCurrent(); voice.cancel(); advanceUntilIdle()
            assertFalse(h.focus.held)
            h.stt.delayMs = 0
            h.media.reject = iteration % 2 == 0
            h.tools.execute(ToolRequest("media.next"))
            assertTrue(h.contacts.dialed.isEmpty() && h.navigation.calls.isEmpty())
        }
        assertEquals(0, h.ai.calls)
    }
    @Test fun repeatedObdLossNoDataAndSimulatorReturnCloseTransport() = runTest {
        val fake = FakeBluetoothTransport()
        val provider = BluetoothObdProvider(fake, DeterministicClock(), backgroundScope)
        repeat(100) {
            fake.exchangeFailure = null
            fake.replies[ObdWireRequest.RPM] = "NO DATA >"
            provider.connect("fixture"); runCurrent()
            assertNull(provider.readings.value.engineRpm.value)
            fake.exchangeFailure = IOException("fixture loss")
            advanceTimeBy(2_100); runCurrent()
            provider.disconnect(true)
            assertFalse(fake.connected)
            assertEquals(VehicleConnectionState.SIMULATED, provider.state.value)
            assertTrue(provider.readings.value.simulationMode)
        }
        assertTrue(fake.closes >= 100)
        assertTrue(fake.sent.all { it in ObdWireRequest.entries })
    }
}
