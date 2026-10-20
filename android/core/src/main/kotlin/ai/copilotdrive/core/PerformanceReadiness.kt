package ai.copilotdrive.core

enum class MeasurementKind { INSTRUMENTED, EXTERNAL_UPPER_BOUND, FUNCTIONAL, HUMAN }
enum class StartupMode { COLD, WARM, HOT }
enum class PerformanceScenario { COLD_STARTUP, WARM_STARTUP, HOT_STARTUP, DASHBOARD_SCROLL, PRESET_DRAFT, SETTINGS, DEVICE_LAB, BACKGROUND_RESTORE }
enum class MeasurementUnit { MILLISECONDS, KIBIBYTES }
enum class BenchmarkMetric { TTID, TTFD, ACTIVITY_START, UI_READY_UPPER_BOUND, FRAME_CPU, FRAME_OVERRUN, PSS, OPERATION_UPPER_BOUND }

data class BenchmarkEnvironment(
    val commit: String, val apkSha256: String, val device: QualificationEnvironment,
    val variant: String, val compilation: String, val displayConditions: String,
) {
    init {
        require(commit.matches(Regex("[0-9a-fA-F]{40}")))
        require(apkSha256.matches(Regex("[0-9a-fA-F]{64}")))
        require(variant.isNotBlank() && compilation.isNotBlank() && displayConditions.isNotBlank())
    }
    /** APK/commit intentionally differ in before/after comparisons; all conditions must agree. */
    fun comparableTo(other: BenchmarkEnvironment) = device == other.device && variant == other.variant &&
        compilation == other.compilation && displayConditions == other.displayConditions
}

data class BenchmarkSample(val scenario: PerformanceScenario, val metric: BenchmarkMetric, val iteration: Int, val value: Double,
    val unit: MeasurementUnit, val kind: MeasurementKind) {
    init {
        require(iteration >= 0 && value.isFinite() && (value >= 0 || metric == BenchmarkMetric.FRAME_OVERRUN))
        require((metric == BenchmarkMetric.PSS) == (unit == MeasurementUnit.KIBIBYTES))
    }
}

data class StartupMeasurement(val mode: StartupMode, val timeToInitialDisplayMs: Double?,
    val timeToFullDisplayMs: Double?, val externalDashboardUpperBoundMs: Double? = null) {
    init {
        listOfNotNull(timeToInitialDisplayMs, timeToFullDisplayMs, externalDashboardUpperBoundMs)
            .forEach { require(it.isFinite() && it >= 0) }
        require(timeToFullDisplayMs == null || (timeToInitialDisplayMs != null && timeToFullDisplayMs >= timeToInitialDisplayMs))
    }
}

data class FrameTimingMeasurement(val androidApi: Int, val frameCount: Int,
    val cpuDurationP95Ms: Double?, val overrunP95Ms: Double?) {
    init {
        require(androidApi >= 1 && frameCount >= 0)
        require(cpuDurationP95Ms == null || (cpuDurationP95Ms.isFinite() && cpuDurationP95Ms >= 0))
        require(overrunP95Ms == null || (androidApi >= 31 && overrunP95Ms.isFinite()))
        require(frameCount > 0 || (cpuDurationP95Ms == null && overrunP95Ms == null))
    }
}
data class MemoryMeasurement(val elapsedMs: Long, val pssKiB: Long) {
    init { require(elapsedMs >= 0 && pssKiB > 0) }
}
enum class StabilityScenario { RESTART, FOREGROUND, PRESET_DRAFT, DEVICE_LAB, OBD_FAKE_LOSS,
    LOCAL_JARVIS, TTS_FAKE_FAILURE, MEDIA_FAKE_SWITCH, PLACES_UNCONFIGURED, RECREATE, PERMISSION_REVOCATION }

/** Advisory only. Never compare an external bound to an instrumented metric. */
data class RegressionThreshold(val allowedIncreaseFraction: Double, val minimumSamples: Int = 20) {
    init { require(allowedIncreaseFraction.isFinite() && allowedIncreaseFraction >= 0 && minimumSamples >= 20) }
    fun exceeded(beforeEnvironment: BenchmarkEnvironment, afterEnvironment: BenchmarkEnvironment,
        before: List<BenchmarkSample>, after: List<BenchmarkSample>): Boolean? {
        if (!beforeEnvironment.comparableTo(afterEnvironment) || before.size < minimumSamples || after.size < minimumSamples) return null
        val all = before + after
        if (all.map { listOf(it.scenario, it.metric, it.unit, it.kind) }.distinct().size != 1 ||
            before.map { it.iteration }.distinct().size != before.size || after.map { it.iteration }.distinct().size != after.size) return null
        fun median(samples: List<BenchmarkSample>) = samples.map { it.value }.sorted()[(samples.size - 1) / 2]
        val baseline = median(before)
        if (baseline <= 0.0) return null
        return median(after) > baseline * (1 + allowedIncreaseFraction)
    }
}

data class PhysicalTestRecord(val qualification: DeviceQualification, val humanConfirmed: Boolean) {
    init {
        require(qualification.environment.kind == QualificationEnvironmentKind.PHYSICAL)
        if (qualification.result.status == CompatibilityStatus.PHYSICALLY_VERIFIED) require(humanConfirmed)
    }
}
enum class ReadinessCheck { FUNCTIONAL, SECURITY, DEBUG_APK, BENCHMARK, STABILITY, API26, API30,
    PHYSICAL_VOICE, PHYSICAL_OBD, PHYSICAL_MEDIA, PHYSICAL_NAVIGATION, COMMERCIAL_AI, COMMERCIAL_PLACES, PRODUCTION_SIGNING }
enum class ReadinessState { PASS, FAIL, PENDING, BLOCKED }
data class ReleaseCandidateReadiness(val checks: Map<ReadinessCheck, ReadinessState>, val physical: List<PhysicalTestRecord>) {
    fun state(check: ReadinessCheck) = checks[check] ?: ReadinessState.PENDING
    fun physicalStatus(scenarioId: String): CompatibilityStatus = physical.lastOrNull {
        it.qualification.scenario.id == scenarioId
    }?.qualification?.result?.status ?: CompatibilityStatus.PENDING
}

/** Readiness describes usable text controls; optional voice/network availability never gates it. */
fun dashboardReady(preferencesLoaded: Boolean, onboarded: Boolean, layoutLoaded: Boolean, firstSnapshot: Boolean) =
    preferencesLoaded && onboarded && layoutLoaded && firstSnapshot
