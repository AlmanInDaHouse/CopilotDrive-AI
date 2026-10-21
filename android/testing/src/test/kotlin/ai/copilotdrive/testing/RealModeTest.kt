package ai.copilotdrive.testing

import ai.copilotdrive.core.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RealModeTest {
    @Test fun `provider starts disconnected and never sends commands automatically`() = runTest {
        val transport = FakeBluetoothTransport()
        val provider = BluetoothObdProvider(transport, DeterministicClock(), backgroundScope)
        assertEquals(VehicleConnectionState.DISCONNECTED, provider.state.value)
        repeat(3) { assertFalse(provider.snapshot().simulationMode); assertNull(provider.snapshot().fuelLevelPercent.value) }
        assertTrue(transport.sent.isEmpty())
        provider.disconnect(true)
        assertTrue(provider.snapshot().simulationMode)
        provider.disconnect()
        assertNull(provider.snapshot().fuelLevelPercent.value)
    }
    @Test fun `jarvis explains disconnected real fuel without AI or synthetic value`() = runTest {
        val h = TestHarness()
        val provider = BluetoothObdProvider(FakeBluetoothTransport(), h.clock, backgroundScope)
        h.vehicle.value = provider.snapshot()
        val reply = h.ask("Jarvis, cuánta gasolina queda")
        assertEquals("DISCONNECTED", reply.errorCode)
        assertTrue(reply.message.contains("todavía no estoy conectado"))
        assertFalse(reply.message.contains("42"))
        assertEquals(0, h.ai.calls)
    }
    @Test fun `real fuel distinguishes unsupported missing and expired`() = runTest {
        val h = TestHarness()
        val disconnected = BluetoothObdProvider(FakeBluetoothTransport(), h.clock, backgroundScope).snapshot()
        h.vehicle.value = disconnected.copy(connectionStatus = ConnectionStatus.CONNECTED)
        assertEquals("UNSUPPORTED", h.ask("gasolina").errorCode)
        h.vehicle.value = h.vehicle.value.copy(supportedCapabilities = setOf("012F")); h.context.invalidate()
        assertEquals("UNAVAILABLE", h.ask("gasolina").errorCode)
        h.vehicle.value = h.vehicle.value.copy(fuelLevelPercent = h.vehicle.value.fuelLevelPercent.copy(value = 31.0,
            capturedAt = h.clock.now() - 11_000, availability = Availability.AVAILABLE)); h.context.invalidate()
        assertEquals("STALE", h.ask("gasolina").errorCode)
        assertFalse(h.ask("gasolina").message.contains("31"))
    }
}
