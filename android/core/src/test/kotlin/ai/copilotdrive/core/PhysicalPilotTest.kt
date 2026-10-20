package ai.copilotdrive.core

import org.junit.Assert.*
import org.junit.Test

class PhysicalPilotTest {
    private val environment = QualificationEnvironment(QualificationEnvironmentKind.PHYSICAL, 26, "test device")
    private val session = PhysicalPilotSession("pilot", "a".repeat(40), "b".repeat(64), TestDeviceProfile("phone-1", environment, 26), 100)
    private val human = HumanObservation("pilot", "tts", 1, HumanOutcome.CONFIRMED, 101, "acta-1")
    private fun physical() = DeviceQualification(session.commit, session.apkSha256, environment,
        QualificationScenario("tts", listOf("authorized stationary test; audible phrase"), ValidationMethod.MANUAL_OBSERVATION),
        QualificationResult(CompatibilityStatus.PHYSICALLY_VERIFIED, EvidenceLevel.ANDROID_DEVICE, "HEARD", "acta-1", true))

    @Test fun physicalVoiceRequiresHumanAndCallbackOfSameAttempt() {
        assertTrue(VoiceQualificationResult(session, physical(), AcousticConfirmation(1, true, human)).acoustic.heard)
        assertThrows(IllegalArgumentException::class.java) { VoiceQualificationResult(session, physical(), AcousticConfirmation(1, true)) }
        assertThrows(IllegalArgumentException::class.java) { VoiceQualificationResult(session, physical(), AcousticConfirmation(1, false, human)) }
        assertThrows(IllegalArgumentException::class.java) { AcousticConfirmation(2, true, human) }
        assertThrows(IllegalArgumentException::class.java) { VoiceQualificationResult(session, physical(), AcousticConfirmation(1, true, human.copy(outcome = HumanOutcome.NOT_OBSERVED))) }
    }
    @Test fun mismatchedSessionBuildScenarioTimestampAndRecordCannotQualify() {
        val q = physical()
        for (observation in listOf(human.copy(sessionId = "other"), human.copy(scenarioId = "stt"),
            human.copy(observedAtMs = 99), human.copy(recordReference = "other"))) {
            assertThrows(IllegalArgumentException::class.java) { VoiceQualificationResult(session, q, AcousticConfirmation(1, true, observation)) }
        }
        assertThrows(IllegalArgumentException::class.java) { VoiceQualificationResult(session.copy(commit = "c".repeat(40)), q, AcousticConfirmation(1, true, human)) }
        assertThrows(IllegalArgumentException::class.java) { VoiceQualificationResult(session.copy(apkSha256 = "c".repeat(64)), q, AcousticConfirmation(1, true, human)) }
    }
    @Test fun integrationRequiresObservedEffectAndMissingGatesStayPending() {
        assertThrows(IllegalArgumentException::class.java) { IntegrationQualification(session, physical()) }
        IntegrationQualification(session, physical(), human)
        assertEquals(CompatibilityStatus.PENDING, PilotReadinessResult(session, emptyList()).status("obd"))
        assertThrows(IllegalArgumentException::class.java) { PilotReadinessResult(session, listOf(physical())) }
        val observed = IntegrationQualification(session, physical(), human)
        assertEquals(CompatibilityStatus.PHYSICALLY_VERIFIED,
            PilotReadinessResult(session, listOf(physical()), integrations = listOf(observed)).status("tts"))
        assertThrows(IllegalArgumentException::class.java) { PilotReadinessResult(session, listOf(physical(), physical())) }
    }
    @Test fun minSdkAndHostResourcesGateMeasurements() {
        assertFalse(session.device.copy(minSdk = 27).supported)
        val benchmark = BenchmarkEnvironment(session.commit, session.apkSha256, environment, "benchmark", "Ignore", "fixed")
        val ready = PerformanceEnvironment(benchmark, 1024, 2048, 0, false, hostGuardPassed = true)
        assertTrue(ready.measurementAllowed)
        assertFalse(ready.copy(availableHostMiB = 326).measurementAllowed)
        assertFalse(ready.copy(commitHeadroomMiB = null).measurementAllowed)
        assertFalse(ready.copy(buildRunning = true).measurementAllowed)
        assertFalse(ready.copy(activeAvds = 2).measurementAllowed)
        assertFalse(ready.copy(hostGuardPassed = false).measurementAllowed)
    }
}
