package ai.copilotdrive.core

import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class ContractTest {
    private val now = 1_700_000_000_000L

    @Test fun `snapshot round trip preserves nullable metrics and provenance`() {
        val snapshot = simulatedVehicle(now)
        val encoded = ContractJson.format.encodeToString(snapshot)
        assertEquals(snapshot, ContractJson.decodeVehicle(encoded))
        assertNull(snapshot.vehicleRangeKm.value)
        assertEquals(Availability.SIMULATED, snapshot.fuelLevelPercent.availability)
    }

    @Test fun `future schema and extra fields are rejected`() {
        val encoded = ContractJson.format.encodeToString(simulatedVehicle(now))
        assertThrows(IllegalArgumentException::class.java) {
            ContractJson.decodeVehicle(encoded.replace("\"schemaVersion\":1", "\"schemaVersion\":2"))
        }
        assertThrows(Exception::class.java) { ContractJson.decodeVehicle(encoded.dropLast(1) + ",\"ecu\":true}") }
        assertThrows(IllegalArgumentException::class.java) { ContractJson.decodeVehicle(encoded.replace("\"schemaVersion\":1,", "")) }
    }

    @Test fun `context expires data and never treats a future timestamp as fresh`() {
        val metric = Metric(42.0, "%", "obd", now, Availability.AVAILABLE)
        assertEquals(Availability.AVAILABLE, metric.at(now + 10_000).availability)
        assertEquals(Availability.STALE, metric.at(now + 10_001).availability)
        assertEquals(Availability.STALE, metric.at(now - 1).availability)
        assertEquals(Availability.DISCONNECTED, metric.at(now, connected = false).availability)
    }

    @Test fun `invalid metric values cannot become valid telemetry`() {
        assertThrows(IllegalArgumentException::class.java) { Metric(Double.NaN, "%", "obd", now, Availability.AVAILABLE) }
        assertThrows(IllegalArgumentException::class.java) { Metric(null, "%", "obd", now, Availability.AVAILABLE) }
        val snapshot = simulatedVehicle(now)
        assertThrows(IllegalArgumentException::class.java) { snapshot.copy(simulationMode = false) }
        assertThrows(IllegalArgumentException::class.java) { snapshot.copy(fuelLevelPercent = snapshot.fuelLevelPercent.copy(unit = "km")) }
    }

    @Test fun `navigation encodes destination instead of interpreting it as a URI`() {
        val uri = NavigationUris.maps("Gran Vía & A?x=#Madrid")
        assertTrue(uri.contains("%26"))
        assertTrue(uri.contains("%3F"))
        assertFalse(uri.contains("#Madrid"))
        for (input in listOf("intent://evil", "https://evil", "x\ny", "", "javascript:alert(1)")) {
            assertThrows(IllegalArgumentException::class.java) { NavigationUris.maps(input) }
        }
    }

    @Test fun `read only PID parser rejects truncated and unsupported packets`() {
        assertEquals(1726.0, ObdDecoder.decode(ReadPid.RPM, "41 0C 1A F8 >")!!, 0.01)
        assertNull(ObdDecoder.decode(ReadPid.RPM, "41 0C 1A >"))
        assertNull(ObdDecoder.decode(ReadPid.FUEL, "NO DATA"))
        assertEquals(setOf("010C", "010D", "0105", "012F"), ReadPid.entries.map { it.command }.toSet())
    }
}
