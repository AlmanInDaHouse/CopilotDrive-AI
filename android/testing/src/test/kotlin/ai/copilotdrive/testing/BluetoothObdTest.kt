package ai.copilotdrive.testing

import ai.copilotdrive.core.*
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class BluetoothObdTest {
    @Test fun `parser handles echo spaces no data partial and malformed packets`() {
        assertEquals(1726.0, ObdResponseParser.metric(ObdWireRequest.RPM, "010C\r41 0C 1A F8\r>"))
        assertEquals(1726.0, ObdResponseParser.metric(ObdWireRequest.RPM, "410C1AF8>"))
        for (raw in listOf("NO DATA >", "41 0C 1A >", "41 0C GG F8 >", "41 0C 1A F8 FF >"))
            assertNull(ObdResponseParser.metric(ObdWireRequest.RPM, raw))
    }

    @Test fun `capability mask excludes unsupported fuel`() {
        val supported = ObdPidRegistry.supported("41 00 08 18 00 00 >", 0)!!
        assertEquals(setOf(ObdWireRequest.RPM, ObdWireRequest.SPEED, ObdWireRequest.COOLANT), supported)
        assertFalse(ObdPidRegistry.hasNext("41 00 08 18 00 00 >", 0))
    }

    @Test fun `fixed initialization and real readings use allowed requests`() = runTest {
        val fake = FakeBluetoothTransport()
        val clock = DeterministicClock()
        val provider = BluetoothObdProvider(fake, clock, backgroundScope)
        provider.connect("00:11:22:33:44:55")
        runCurrent()
        assertEquals(VehicleConnectionState.CONNECTED_REAL, provider.state.value)
        assertFalse(provider.readings.value.simulationMode)
        assertEquals(1726.0, provider.readings.value.engineRpm.value)
        assertNull(provider.readings.value.vehicleRangeKm.value)
        assertEquals(listOf(ObdWireRequest.RESET, ObdWireRequest.ECHO_OFF, ObdWireRequest.LINEFEEDS_OFF,
            ObdWireRequest.SPACES_ON, ObdWireRequest.AUTO_PROTOCOL, ObdWireRequest.CAPABILITIES_00,
            ObdWireRequest.CAPABILITIES_20), fake.sent.take(7))
        provider.disconnect(true)
        assertEquals(VehicleConnectionState.SIMULATED, provider.state.value)
        assertTrue(fake.closes > 0)
    }

    @Test fun `no data and unsupported PID remain unavailable`() = runTest {
        val fake = FakeBluetoothTransport().apply {
            replies[ObdWireRequest.CAPABILITIES_00] = "41 00 08 18 00 00 >"
            replies[ObdWireRequest.RPM] = "NO DATA >"
        }
        val provider = BluetoothObdProvider(fake, DeterministicClock(), backgroundScope)
        provider.connect("device")
        runCurrent()
        assertNull(provider.readings.value.engineRpm.value)
        assertNull(provider.readings.value.fuelLevelPercent.value)
        assertFalse(ObdWireRequest.FUEL in fake.sent)
        provider.disconnect()
    }

    @Test fun `connection failure retries twice and disconnects`() = runTest {
        val fake = FakeBluetoothTransport().apply { openFailure = IOException("adapter gone") }
        val provider = BluetoothObdProvider(fake, DeterministicClock(), backgroundScope)
        provider.connect("device")
        advanceTimeBy(1_001); runCurrent()
        assertEquals(2, fake.opens)
        assertEquals(VehicleConnectionState.ERROR, provider.state.value)
        assertEquals(ConnectionStatus.DISCONNECTED, provider.readings.value.connectionStatus)
    }

    @Test fun `permission revocation radio absence and cancellation release transport`() = runTest {
        for (fake in listOf(FakeBluetoothTransport().apply { permissionGranted = false },
            FakeBluetoothTransport().apply { radioPresent = false })) {
            val provider = BluetoothObdProvider(fake, DeterministicClock(), backgroundScope)
            provider.connect("device"); advanceTimeBy(1_001); runCurrent()
            assertEquals(VehicleConnectionState.ERROR, provider.state.value)
        }
        val slow = FakeBluetoothTransport().apply { connectLatencyMs = 10_000 }
        val provider = BluetoothObdProvider(slow, DeterministicClock(), backgroundScope)
        provider.connect("device"); runCurrent(); provider.disconnect()
        assertEquals(VehicleConnectionState.DISCONNECTED, provider.state.value)
        assertFalse(slow.connected)
    }

    @Test fun `slow response times out and a new session can reconnect`() = runTest {
        val fake = FakeBluetoothTransport().apply { latencyMs = 9_000 }
        val provider = BluetoothObdProvider(fake, DeterministicClock(), backgroundScope)
        provider.connect("device")
        advanceTimeBy(17_100); runCurrent()
        assertEquals(VehicleConnectionState.ERROR, provider.state.value)
        assertEquals(2, fake.opens)
        fake.latencyMs = 0
        provider.connect("device"); runCurrent()
        assertEquals(VehicleConnectionState.CONNECTED_REAL, provider.state.value)
        provider.disconnect()
    }

    @Test fun `malformed discovery never publishes a real value`() = runTest {
        val fake = FakeBluetoothTransport().apply { replies[ObdWireRequest.CAPABILITIES_00] = "41 00 08 18 >" }
        val provider = BluetoothObdProvider(fake, DeterministicClock(), backgroundScope)
        provider.connect("device")
        advanceTimeBy(1_001); runCurrent()
        assertEquals(VehicleConnectionState.ERROR, provider.state.value)
        assertNull(provider.readings.value.fuelLevelPercent.value)
        assertFalse(provider.readings.value.simulationMode)
    }

    @Test fun `permission revoked after a reading clears current telemetry`() = runTest {
        val fake = FakeBluetoothTransport()
        val provider = BluetoothObdProvider(fake, DeterministicClock(), backgroundScope)
        provider.connect("device"); runCurrent()
        assertNotNull(provider.readings.value.fuelLevelPercent.value)
        fake.permissionGranted = false
        advanceTimeBy(1_001); runCurrent()
        assertEquals(VehicleConnectionState.STALE, provider.state.value)
        assertNull(provider.readings.value.fuelLevelPercent.value)
        advanceTimeBy(1_001); runCurrent()
        assertEquals(VehicleConnectionState.ERROR, provider.state.value)
    }
}
