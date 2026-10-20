package ai.copilotdrive.core

import org.junit.Assert.*
import org.junit.Test

class DeviceQualificationTest {
    private val env = QualificationEnvironment(QualificationEnvironmentKind.EMULATOR, 35, "API35 x86_64")
    private val scenario = QualificationScenario("dashboard", listOf("onboarded; offline"), ValidationMethod.INSTRUMENTATION)
    private val result = QualificationResult(CompatibilityStatus.EMULATOR_TESTED, EvidenceLevel.EMULATOR, "PASS", "run.json")
    private fun record(environment: QualificationEnvironment = env, check: QualificationScenario = scenario, outcome: QualificationResult = result) =
        DeviceQualification("a".repeat(40), "b".repeat(64), environment, check, outcome)
    @Test fun emulatorCannotBecomePhysicalEvenWithManualFlag() {
        assertThrows(IllegalArgumentException::class.java) { record(check = scenario.copy(method = ValidationMethod.MANUAL_OBSERVATION),
            outcome = result.copy(status = CompatibilityStatus.PHYSICALLY_VERIFIED, evidence = EvidenceLevel.ANDROID_DEVICE, physicallyObserved = true)) }
    }
    @Test fun deviceCallbackAloneIsNotPhysicalVerification() {
        assertThrows(IllegalArgumentException::class.java) { record(env.copy(kind = QualificationEnvironmentKind.PHYSICAL),
            outcome = result.copy(status = CompatibilityStatus.PHYSICALLY_VERIFIED, evidence = EvidenceLevel.ANDROID_DEVICE)) }
    }
    @Test fun observedPhysicalScenarioRequiresCompleteProvenance() {
        assertEquals(CompatibilityStatus.PHYSICALLY_VERIFIED, record(env.copy(kind = QualificationEnvironmentKind.PHYSICAL),
            scenario.copy(method = ValidationMethod.MANUAL_OBSERVATION), result.copy(status = CompatibilityStatus.PHYSICALLY_VERIFIED,
                evidence = EvidenceLevel.ANDROID_DEVICE, physicallyObserved = true)).result.status)
        assertThrows(IllegalArgumentException::class.java) { record().copy(apkSha256 = "unknown") }
        assertThrows(IllegalArgumentException::class.java) { env.copy(androidApi = null) }
    }
    @Test fun pendingAndFailedDoNotClaimCompatibility() {
        for (status in listOf(CompatibilityStatus.PENDING, CompatibilityStatus.FAILED, CompatibilityStatus.BLOCKED, CompatibilityStatus.UNSUPPORTED))
            assertEquals(status, record(outcome = result.copy(status = status)).result.status)
    }
    @Test fun percentilesRequireTwentyHomogeneousSamples() {
        val samples = (1..20).map { PerformanceSample(PerformanceMetric.ACTIVITY_START_MS, it.toLong(), it) }
        assertNull(performanceSummary(samples.take(19)).p95Ms)
        assertEquals(LatencySummary(20, 10, 19), performanceSummary(samples))
        assertThrows(IllegalArgumentException::class.java) { performanceSummary(samples + PerformanceSample(PerformanceMetric.MEMORY_PSS_KB, 1, 21)) }
        assertThrows(IllegalArgumentException::class.java) { PerformanceSample(PerformanceMetric.ACTIVITY_START_MS, -1, 0) }
    }
}
