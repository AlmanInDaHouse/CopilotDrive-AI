package ai.copilotdrive.core

/** Compatibility is scoped to one scenario and one exact build, never to a product family. */
enum class CompatibilityStatus { AUTOMATED_TESTED, EMULATOR_TESTED, PHYSICALLY_VERIFIED, FAILED, UNSUPPORTED, BLOCKED, PENDING }
enum class QualificationEnvironmentKind { HOST, EMULATOR, PHYSICAL }
enum class ValidationMethod { JVM_TEST, INSTRUMENTATION, ADB_MEASUREMENT, MANUAL_OBSERVATION }

data class QualificationEnvironment(val kind: QualificationEnvironmentKind, val androidApi: Int?, val model: String) {
    init { require(model.isNotBlank()); require(androidApi == null || androidApi >= 1)
        require(kind == QualificationEnvironmentKind.HOST || androidApi != null) }
}
data class QualificationScenario(val id: String, val preconditions: List<String>, val method: ValidationMethod) {
    init { require(id.isNotBlank() && preconditions.isNotEmpty() && preconditions.none { it.isBlank() }) }
}
data class QualificationResult(
    val status: CompatibilityStatus,
    val evidence: EvidenceLevel,
    val observationCode: String,
    /** Reference to a redacted execution record; never a transcript, serial, token or precise location. */
    val recordReference: String,
    val physicallyObserved: Boolean = false,
) { init { require(observationCode.matches(Regex("[A-Z0-9_]{1,80}"))); require(recordReference.isNotBlank()) } }

data class DeviceQualification(
    val commit: String, val apkSha256: String, val environment: QualificationEnvironment,
    val scenario: QualificationScenario, val result: QualificationResult,
) {
    init {
        require(commit.matches(Regex("[0-9a-fA-F]{40}")))
        require(apkSha256.matches(Regex("[0-9a-fA-F]{64}")))
        when (result.status) {
            CompatibilityStatus.PHYSICALLY_VERIFIED -> {
                require(environment.kind == QualificationEnvironmentKind.PHYSICAL)
                require(scenario.method == ValidationMethod.MANUAL_OBSERVATION && result.physicallyObserved)
                require(result.evidence in setOf(EvidenceLevel.ANDROID_DEVICE, EvidenceLevel.OBD_ADAPTER, EvidenceLevel.PARKED_VEHICLE))
            }
            CompatibilityStatus.EMULATOR_TESTED -> require(environment.kind == QualificationEnvironmentKind.EMULATOR && result.evidence == EvidenceLevel.EMULATOR)
            CompatibilityStatus.AUTOMATED_TESTED -> require(scenario.method != ValidationMethod.MANUAL_OBSERVATION &&
                result.evidence in setOf(EvidenceLevel.UNIT_TEST, EvidenceLevel.FAKE))
            else -> Unit
        }
        require(!result.physicallyObserved || environment.kind == QualificationEnvironmentKind.PHYSICAL)
    }
}

enum class PerformanceMetric { ACTIVITY_START_MS, DASHBOARD_READY_UPPER_BOUND_MS, MEMORY_PSS_KB, LOCAL_OPERATION_MS, VOICE_STATE_MS }
data class PerformanceSample(val metric: PerformanceMetric, val value: Long, val iteration: Int) {
    init { require(value >= 0 && iteration >= 0) }
}
/** Nearest-rank percentiles. Like voice diagnostics, insufficient observations show count only. */
fun performanceSummary(samples: List<PerformanceSample>): LatencySummary {
    require(samples.map { it.metric }.distinct().size <= 1)
    val values = samples.map { it.value }.sorted()
    return if (values.size < 20) LatencySummary(values.size) else LatencySummary(values.size,
        values[(kotlin.math.ceil(values.size * 0.50).toInt() - 1)],
        values[(kotlin.math.ceil(values.size * 0.95).toInt() - 1)])
}
data class AccessibilityFinding(val id: String, val component: String, val observationCode: String,
    val status: CompatibilityStatus, val testReference: String)
data class RegressionRecord(val id: String, val scenarioId: String, val beforeCode: String, val afterCode: String,
    val testReference: String)
