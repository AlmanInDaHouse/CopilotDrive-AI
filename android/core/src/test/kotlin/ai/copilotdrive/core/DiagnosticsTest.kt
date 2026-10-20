package ai.copilotdrive.core

import org.junit.Assert.*
import org.junit.Test

class DiagnosticsTest {
    @Test fun latencyPercentilesRequireEnoughObservations() {
        val tracker = VoiceLatencyTracker(20)
        (1L..19L).forEach { tracker.record(VoiceMetric.RECOGNITION, it) }
        assertEquals(19, tracker.summaries.value[VoiceMetric.RECOGNITION]?.count)
        assertNull(tracker.summaries.value[VoiceMetric.RECOGNITION]?.p50Ms)
        tracker.record(VoiceMetric.RECOGNITION, 20)
        assertEquals(10L, tracker.summaries.value[VoiceMetric.RECOGNITION]?.p50Ms)
        assertEquals(19L, tracker.summaries.value[VoiceMetric.RECOGNITION]?.p95Ms)
        assertNull(tracker.summaries.value[VoiceMetric.TTS_CALLBACK])
    }
    @Test fun historyBoundsAndRedactsSensitiveEvidence() {
        val history = DiagnosticHistory(2)
        repeat(3) { index -> history.record(DiagnosticCheck("check$index", "obd", DiagnosticStatus.FAIL,
            EvidenceLevel.FAKE, 100L + index, description = "AA:BB:CC:DD:EE:FF VIN 1HGCM82633A004352 Bearer sk-supersecret12345")) }
        assertEquals(2, history.checks.value.size)
        assertEquals("check1", history.checks.value.first().id)
        val description = history.checks.value.last().description
        assertFalse(description.contains("AA:BB"))
        assertFalse(description.contains("1HGCM"))
        assertFalse(description.contains("sk-supersecret"))
        history.clear()
        assertTrue(history.checks.value.isEmpty())
    }

    @Test fun contextGrantRequiresFreshSeparateConsentAndMinimization() {
        val grant = VehicleContextGrant("obd", 100, 50, "reported", "rpm", true, 100, false)
        assertTrue(grant.permits(150, 100))
        assertFalse(grant.permits(151, 100))
        assertFalse(grant.copy(specificallyConsented = false).permits(110, 10))
        assertFalse(grant.copy(revoked = true).permits(110, 10))
        assertFalse(grant.copy(category = "location").permits(110, 10))
        assertFalse(grant.permits(110, 101))
    }
}
