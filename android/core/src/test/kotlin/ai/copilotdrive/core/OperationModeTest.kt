package ai.copilotdrive.core

import org.junit.Assert.*
import org.junit.Test

class OperationModeTest {
    @Test fun `new install defaults real without migration notice`() {
        assertEquals(ModeMigration(OperationMode.REAL, false), migrateOperationMode(null, false))
    }
    @Test fun `legacy defaults were not explicit demo decisions`() {
        assertEquals(ModeMigration(OperationMode.REAL, true), migrateOperationMode(null, true))
    }
    @Test fun `explicit choice survives subsequent loads`() {
        for (mode in OperationMode.entries) assertEquals(ModeMigration(mode, false), migrateOperationMode(mode.name, true))
        assertEquals(OperationMode.REAL, migrateOperationMode("corrupt", true).mode)
    }
    @Test fun `unsupported missing and stale are distinct without changing wire schema`() {
        val demo = simulatedVehicle(100)
        fun real(m: Metric) = m.copy(source = "obd", availability = Availability.UNAVAILABLE, value = null, quality = Quality.UNKNOWN)
        val real = demo.copy(source = "obd", simulationMode = false, supportedCapabilities = setOf("012F"),
            speedKmh = real(demo.speedKmh), engineRpm = real(demo.engineRpm), fuelLevelPercent = real(demo.fuelLevelPercent),
            coolantTemperatureC = real(demo.coolantTemperatureC), batteryVoltage = real(demo.batteryVoltage), vehicleRangeKm = real(demo.vehicleRangeKm))
        assertEquals("No disponible", telemetryLabel(real, real.fuelLevelPercent, "012F"))
        assertEquals("No compatible", telemetryLabel(real, real.coolantTemperatureC, "0105"))
        val stale = real.fuelLevelPercent.copy(value = 24.0, availability = Availability.STALE)
        assertEquals("Dato obsoleto", telemetryLabel(real, stale, "012F"))
        assertEquals("No disponible", telemetryLabel(real.copy(connectionStatus = ConnectionStatus.DISCONNECTED), stale, "012F"))
        assertEquals("42 %", telemetryLabel(demo, demo.fuelLevelPercent, "012F"))
    }
}
