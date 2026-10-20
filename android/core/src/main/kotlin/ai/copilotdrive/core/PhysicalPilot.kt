package ai.copilotdrive.core

data class TestDeviceProfile(val alias: String, val environment: QualificationEnvironment, val minSdk: Int) {
    init { require(alias.matches(Regex("[a-zA-Z0-9_-]{1,40}")) && minSdk >= 1) }
    val supported get() = environment.androidApi?.let { it >= minSdk } ?: false
}

data class PhysicalPilotSession(val id: String, val commit: String, val apkSha256: String,
    val device: TestDeviceProfile, val startedAtMs: Long) {
    init {
        require(id.matches(Regex("[a-zA-Z0-9_-]{1,80}")) && startedAtMs > 0)
        require(commit.matches(Regex("[0-9a-fA-F]{40}")) && apkSha256.matches(Regex("[0-9a-fA-F]{64}")))
    }
    fun matches(qualification: DeviceQualification) = commit.equals(qualification.commit, true) &&
        apkSha256.equals(qualification.apkSha256, true) && device.environment == qualification.environment
}

enum class HumanOutcome { PENDING, CONFIRMED, NOT_OBSERVED }
data class HumanObservation(val sessionId: String, val scenarioId: String, val attempt: Long,
    val outcome: HumanOutcome, val observedAtMs: Long, val recordReference: String) {
    init {
        require(sessionId.isNotBlank() && scenarioId.isNotBlank() && attempt > 0 && observedAtMs > 0)
        require(recordReference.isNotBlank())
    }
}

data class AcousticConfirmation(val attempt: Long, val callbackCompleted: Boolean,
    val human: HumanObservation? = null) {
    init { require(attempt > 0); require(human == null || human.attempt == attempt) }
    val heard get() = callbackCompleted && human?.outcome == HumanOutcome.CONFIRMED
}

private fun requirePhysicalObservation(session: PhysicalPilotSession, qualification: DeviceQualification,
    observation: HumanObservation?) {
    require(session.matches(qualification))
    if (observation != null) {
        require(observation.sessionId == session.id && observation.scenarioId == qualification.scenario.id)
        require(observation.observedAtMs >= session.startedAtMs)
    }
    if (qualification.result.status == CompatibilityStatus.PHYSICALLY_VERIFIED) {
        require(session.device.supported && observation?.outcome == HumanOutcome.CONFIRMED)
        require(qualification.result.recordReference == observation.recordReference)
    }
}

data class VoiceQualificationResult(val session: PhysicalPilotSession, val qualification: DeviceQualification,
    val acoustic: AcousticConfirmation) {
    init {
        requirePhysicalObservation(session, qualification, acoustic.human)
        if (qualification.result.status == CompatibilityStatus.PHYSICALLY_VERIFIED) require(acoustic.heard)
    }
}

data class IntegrationQualification(val session: PhysicalPilotSession, val qualification: DeviceQualification,
    val observedEffect: HumanObservation? = null) {
    init { requirePhysicalObservation(session, qualification, observedEffect) }
}

data class PerformanceEnvironment(val benchmark: BenchmarkEnvironment, val availableHostMiB: Long?,
    val commitHeadroomMiB: Long?, val activeAvds: Int, val buildRunning: Boolean, val hostGuardPassed: Boolean = false) {
    init { require(activeAvds >= 0); require(availableHostMiB == null || availableHostMiB >= 0)
        require(commitHeadroomMiB == null || commitHeadroomMiB >= 0) }
    val measurementAllowed get() = availableHostMiB != null && availableHostMiB >= 512 &&
        commitHeadroomMiB != null && commitHeadroomMiB >= 1024 && activeAvds <= 1 && !buildRunning && hostGuardPassed
}

data class PilotReadinessResult(val session: PhysicalPilotSession, val qualifications: List<DeviceQualification>,
    val voice: List<VoiceQualificationResult> = emptyList(), val integrations: List<IntegrationQualification> = emptyList()) {
    init {
        require(qualifications.all(session::matches))
        require(qualifications.map { it.scenario.id }.distinct().size == qualifications.size)
        require(voice.all { it.session == session } && integrations.all { it.session == session })
        val observed = voice.map { it.qualification } + integrations.map { it.qualification }
        require(qualifications.filter { it.result.status == CompatibilityStatus.PHYSICALLY_VERIFIED }.all { it in observed })
    }
    fun status(scenarioId: String) = qualifications.find { it.scenario.id == scenarioId }?.result?.status ?: CompatibilityStatus.PENDING
}

/** No payload can contain a transcript, tool argument, provider text or private reasoning. */
enum class VoiceJourneyStage { REQUEST_STARTED, STT_LISTENING, STT_TRANSCRIBING, TRANSCRIPT_OBTAINED,
    ORCHESTRATOR_PROCESSING, TOOL_REQUESTED, TOOL_AUTHORIZED, TOOL_REJECTED, TEXT_RESPONSE,
    TTS_STARTED, TTS_COMPLETED, FAILED, CANCELLED, PERMISSION_REQUIRED }
