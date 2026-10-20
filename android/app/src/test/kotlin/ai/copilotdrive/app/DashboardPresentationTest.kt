package ai.copilotdrive.app

import ai.copilotdrive.app.ui.metricLabel
import ai.copilotdrive.app.ui.dashboardColumns
import ai.copilotdrive.core.*
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardPresentationTest {
    @Test fun `adaptive columns use available dp and font scale`() {
        assertEquals(1, dashboardColumns(360f, 1f))
        assertEquals(2, dashboardColumns(800f, 1f))
        assertEquals(4, dashboardColumns(1280f, 1f))
        assertEquals(1, dashboardColumns(800f, 1.5f))
        assertEquals(2, dashboardColumns(1920f, 2f))
    }
    @Test fun `stale metric is never rendered as current value`() {
        val metric = Metric(42.0, "%", "obd", 1, Availability.AVAILABLE)
        assertEquals("42 %", metricLabel(metric))
        assertEquals("No disponible", metricLabel(metric.at(20_000)))
        assertEquals("No disponible", metricLabel(metric.at(1, connected = false)))
    }
    @Test fun `absent telemetry and range use explicit unknown label`() {
        assertEquals("No disponible", metricLabel(null))
        assertEquals("No disponible", metricLabel(simulatedVehicle(0).vehicleRangeKm))
    }
}
