package ai.copilotdrive.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VoiceMetric { STT_INIT, RECOGNITION, ORCHESTRATOR, TTS_INIT, TTS_CALLBACK }
data class LatencySummary(val count: Int, val p50Ms: Long? = null, val p95Ms: Long? = null)

class VoiceLatencyTracker(private val minimumSamples: Int = 20) {
    init { require(minimumSamples >= 2) }
    private val samples = mutableMapOf<VoiceMetric, List<Long>>()
    private val mutable = MutableStateFlow<Map<VoiceMetric, LatencySummary>>(emptyMap())
    val summaries = mutable.asStateFlow()

    @Synchronized fun record(metric: VoiceMetric, durationMs: Long) {
        require(durationMs >= 0)
        val values = (samples[metric].orEmpty() + durationMs).takeLast(100)
        samples[metric] = values
        val sorted = values.sorted()
        fun percentile(p: Double): Long = sorted[kotlin.math.ceil(sorted.size * p).toInt().coerceAtLeast(1) - 1]
        mutable.value = mutable.value + (metric to if (values.size >= minimumSamples)
            LatencySummary(values.size, percentile(0.5), percentile(0.95)) else LatencySummary(values.size))
    }
}

enum class DiagnosticStatus { PASS, FAIL, PENDING, UNSUPPORTED, CANCELLED }
enum class EvidenceLevel { UNIT_TEST, FAKE, EMULATOR, ANDROID_DEVICE, OBD_ADAPTER, PARKED_VEHICLE, PENDING }

/** A check describes only what was observed. Availability is never physical verification. */
data class DiagnosticCheck(
    val id: String,
    val component: String,
    val status: DiagnosticStatus,
    val evidence: EvidenceLevel,
    val timestampMs: Long,
    val durationMs: Long? = null,
    val errorCode: String? = null,
    val description: String,
    val correctiveAction: String? = null,
)

/** Session-only history, bounded to avoid retaining device observations indefinitely. */
class DiagnosticHistory(private val capacity: Int = 100) {
    init { require(capacity > 0) }
    private val mutable = MutableStateFlow<List<DiagnosticCheck>>(emptyList())
    val checks = mutable.asStateFlow()

    fun record(check: DiagnosticCheck) {
        require(check.id.isNotBlank() && check.component.isNotBlank())
        require(check.timestampMs >= 0 && (check.durationMs == null || check.durationMs >= 0))
        mutable.value = (mutable.value + check.copy(
            id = DiagnosticRedaction.clean(check.id, 80),
            component = DiagnosticRedaction.clean(check.component, 80),
            errorCode = check.errorCode?.let { DiagnosticRedaction.clean(it, 80) },
            description = DiagnosticRedaction.clean(check.description, 240),
            correctiveAction = check.correctiveAction?.let { DiagnosticRedaction.clean(it, 240) },
        )).takeLast(capacity)
    }

    fun clear() { mutable.value = emptyList() }
}

/** A defensive boundary for display and any future manual export. No raw adapter/provider errors. */
object DiagnosticRedaction {
    private val mac = Regex("(?i)\\b(?:[0-9a-f]{2}:){5}[0-9a-f]{2}\\b")
    private val vin = Regex("(?i)\\b[A-HJ-NPR-Z0-9]{17}\\b")
    private val bearer = Regex("(?i)bearer\\s+[^\\s,;]+")
    private val token = Regex("(?i)(?:sk-[A-Za-z0-9_-]{8,}|api[_-]?key\\s*[:=]\\s*[^\\s,;]+)")
    fun clean(value: String, limit: Int): String = value.replace(mac, "[redacted-mac]")
        .replace(vin, "[redacted-vin]").replace(bearer, "[redacted-token]")
        .replace(token, "[redacted-token]").take(limit)
}

/** Future vehicle context exchange contract. No sender is enabled by this type. */
data class VehicleContextGrant(
    val source: String,
    val capturedAtMs: Long,
    val ttlMs: Long,
    val quality: String,
    val category: String,
    val specificallyConsented: Boolean,
    val maxBytes: Int,
    val revoked: Boolean,
) {
    fun permits(nowMs: Long, bytes: Int): Boolean = specificallyConsented && !revoked &&
        ttlMs > 0 && nowMs >= capturedAtMs && nowMs - capturedAtMs <= ttlMs &&
        bytes in 1..maxBytes && maxBytes <= 16_384 &&
        category !in setOf("contact", "location", "phone", "vin")
}
