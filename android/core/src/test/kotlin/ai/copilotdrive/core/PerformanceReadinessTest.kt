package ai.copilotdrive.core

import org.junit.Assert.*
import org.junit.Test

class PerformanceReadinessTest {
    private val environment = BenchmarkEnvironment("a".repeat(40), "b".repeat(64),
        QualificationEnvironment(QualificationEnvironmentKind.EMULATOR, 35, "test"), "debug", "none", "1280x800/160/1")
    private fun samples(value: Double, kind: MeasurementKind = MeasurementKind.INSTRUMENTED) = (0..19).map {
        BenchmarkSample(PerformanceScenario.COLD_STARTUP, BenchmarkMetric.TTID, it, value, MeasurementUnit.MILLISECONDS, kind)
    }
    @Test fun readinessNeedsAllFourPrerequisites() {
        assertTrue(dashboardReady(true, true, true, true))
        for (missing in 0..3) {
            val flags = MutableList(4) { true }; flags[missing] = false
            assertFalse(dashboardReady(flags[0], flags[1], flags[2], flags[3]))
        }
    }
    @Test fun rejectsInvalidValuesAndWrongTimingOrder() {
        for (value in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) { samples(value) }
        }
        assertThrows(IllegalArgumentException::class.java) { StartupMeasurement(StartupMode.COLD, 100.0, 50.0) }
        assertThrows(IllegalArgumentException::class.java) { StartupMeasurement(StartupMode.COLD, null, 50.0) }
        assertNull(StartupMeasurement(StartupMode.COLD, null, null, 500.0).timeToFullDisplayMs)
        assertThrows(IllegalArgumentException::class.java) { MemoryMeasurement(0, 0) }
    }
    @Test fun frameOverrunAllowsNegativeButRequiresApi31() {
        FrameTimingMeasurement(35, 100, 10.0, -2.0)
        assertThrows(IllegalArgumentException::class.java) { FrameTimingMeasurement(30, 100, 10.0, -2.0) }
        assertThrows(IllegalArgumentException::class.java) { FrameTimingMeasurement(35, 0, 10.0, null) }
    }
    @Test fun regressionRequiresComparableConditionsEnoughSamplesAndSameMetric() {
        val threshold = RegressionThreshold(.1)
        assertEquals(true, threshold.exceeded(environment, environment, samples(100.0), samples(120.0)))
        assertEquals(false, threshold.exceeded(environment, environment, samples(100.0), samples(105.0)))
        assertNull(threshold.exceeded(environment, environment.copy(variant = "benchmark"), samples(100.0), samples(120.0)))
        assertNull(threshold.exceeded(environment, environment, samples(100.0).take(19), samples(120.0)))
        assertNull(threshold.exceeded(environment, environment, samples(100.0), samples(120.0, MeasurementKind.EXTERNAL_UPPER_BOUND)))
        assertNull(threshold.exceeded(environment, environment, samples(100.0), samples(120.0).map { it.copy(metric = BenchmarkMetric.TTFD) }))
        assertNull(threshold.exceeded(environment, environment, samples(100.0), samples(120.0).map { it.copy(scenario = PerformanceScenario.HOT_STARTUP) }))
        assertNull(threshold.exceeded(environment, environment, samples(0.0), samples(1.0)))
    }
    @Test fun absentReadinessAndPhysicalEvidenceStayPending() {
        val readiness = ReleaseCandidateReadiness(mapOf(ReadinessCheck.DEBUG_APK to ReadinessState.PASS), emptyList())
        assertEquals(ReadinessState.PENDING, readiness.state(ReadinessCheck.PHYSICAL_VOICE))
        assertEquals(CompatibilityStatus.PENDING, readiness.physicalStatus("voice"))
        val emulator = DeviceQualification("a".repeat(40), "b".repeat(64), environment.device,
            QualificationScenario("voice", listOf("emulator"), ValidationMethod.INSTRUMENTATION),
            QualificationResult(CompatibilityStatus.EMULATOR_TESTED, EvidenceLevel.EMULATOR, "PASS", "record"))
        assertThrows(IllegalArgumentException::class.java) { PhysicalTestRecord(emulator, true) }
    }
}
