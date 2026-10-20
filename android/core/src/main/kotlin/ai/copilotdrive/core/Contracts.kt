package ai.copilotdrive.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun interface Clock { fun now(): Long }
object SystemClock : Clock { override fun now() = System.currentTimeMillis() }

@Serializable enum class Availability { AVAILABLE, UNAVAILABLE, STALE, DISCONNECTED, SIMULATED }
@Serializable enum class ConnectionStatus { CONNECTED, DISCONNECTED }
@Serializable enum class Quality { UNKNOWN, SENSOR_REPORTED, FIXTURE }

@Serializable
data class Metric(
    val value: Double?,
    val unit: String,
    val source: String,
    val capturedAt: Long,
    val availability: Availability,
    val quality: Quality = Quality.UNKNOWN,
) {
    init {
        require(value == null || value.isFinite())
        require(value != null || availability !in setOf(Availability.AVAILABLE, Availability.SIMULATED))
        require(source.isNotBlank() && capturedAt >= 0)
    }

    fun at(now: Long, connected: Boolean = true, ttlMs: Long = 10_000): Metric = copy(
        availability = when {
            !connected -> Availability.DISCONNECTED
            availability in setOf(Availability.UNAVAILABLE, Availability.DISCONNECTED) -> availability
            now < capturedAt || now - capturedAt > ttlMs -> Availability.STALE
            else -> availability
        },
    )
}

@Serializable
data class VehicleSnapshot(
    val schemaVersion: Int = 1,
    val source: String,
    val connectionStatus: ConnectionStatus,
    val simulationMode: Boolean,
    val capturedAt: Long,
    val speedKmh: Metric,
    val engineRpm: Metric,
    val fuelLevelPercent: Metric,
    val coolantTemperatureC: Metric,
    val batteryVoltage: Metric,
    val vehicleRangeKm: Metric,
    val supportedCapabilities: Set<String>,
) {
    init {
        require(schemaVersion == 1) { "Unsupported schema version" }
        require(capturedAt >= 0 && source.isNotBlank())
        val metrics = listOf(speedKmh, engineRpm, fuelLevelPercent, coolantTemperatureC, batteryVoltage, vehicleRangeKm)
        require(metrics.map { it.unit } == listOf("km/h", "rpm", "%", "°C", "V", "km"))
        require(simulationMode || (source != "simulator" && metrics.none { it.source == "simulator" }))
        require(simulationMode || metrics.none { it.availability == Availability.SIMULATED || it.quality == Quality.FIXTURE })
        require(fuelLevelPercent.value == null || fuelLevelPercent.value in 0.0..100.0)
    }
}

@Serializable enum class PlaybackStatus { PLAYING, PAUSED, STOPPED, UNAVAILABLE }
@Serializable enum class MediaControl { PLAY, PAUSE, NEXT, PREVIOUS, SEEK }
@Serializable
data class MediaSnapshot(
    val sourceApp: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val artworkUri: String? = null,
    val durationMs: Long? = null,
    val positionMs: Long? = null,
    val playbackStatus: PlaybackStatus = PlaybackStatus.UNAVAILABLE,
    val supportedControls: Set<MediaControl> = emptySet(),
    val capturedAt: Long = 0,
    val simulated: Boolean = false,
    val reason: String? = null,
)

@Serializable data class Coordinates(val latitude: Double, val longitude: Double)
@Serializable
data class NavigationSnapshot(
    val provider: String? = null,
    val destinationName: String? = null,
    val origin: Coordinates? = null,
    val destination: Coordinates? = null,
    val remainingDistanceKm: Double? = null,
    val remainingDurationSeconds: Long? = null,
    val navigationStatus: String = "UNAVAILABLE",
    val lastUpdate: Long = 0,
    val dataSource: String = "none",
)

@Serializable enum class InputMode { VOICE, TEXT, DEMO }
@Serializable
data class AssistantRequest(
    val requestId: String,
    val sessionId: String,
    val userText: String,
    val inputMode: InputMode,
    val timestamp: Long,
    val locale: String = "es-ES",
    val contextReferences: Set<String> = emptySet(),
)

@Serializable data class ToolRequest(val toolId: String, val arguments: Map<String, String> = emptyMap())
@Serializable enum class ExecutionStatus { SUCCESS, REQUESTED, DENIED, FAILED, CONFIRMATION_REQUIRED }
@Serializable
data class ToolResult(
    val toolId: String,
    val executionStatus: ExecutionStatus,
    val message: String,
    val errorCode: String? = null,
    val result: Map<String, String> = emptyMap(),
    val contacts: List<Contact> = emptyList(),
) {
    companion object {
        fun denied(id: String, code: String, message: String) = ToolResult(id, ExecutionStatus.DENIED, message, code)
    }
}

@Serializable data class Contact(val id: String, val name: String, val number: String)
@Serializable data class Place(val name: String, val rating: Double?, val distanceKm: Double?, val simulated: Boolean)
enum class CapabilityStatus { AVAILABLE, PERMISSION_REQUIRED, UNSUPPORTED, NOT_CONFIGURED, DISCONNECTED, ERROR }
data class Capability(val id: String, val status: CapabilityStatus, val reason: String)
enum class MotionState { UNKNOWN, MOVING, PARKED }
data class WidgetDefinition(
    val id: String, val type: String, val title: String, val minWidth: Int, val minHeight: Int,
    val capabilities: Set<String>, val configurationSchema: String, val visibilityRules: Set<String>,
)

object ContractJson {
    val format = Json { encodeDefaults = true; ignoreUnknownKeys = false; isLenient = false }
    fun decodeVehicle(text: String): VehicleSnapshot {
        require(format.parseToJsonElement(text).jsonObject["schemaVersion"]?.jsonPrimitive?.intOrNull == 1) { "Explicit schema version 1 required" }
        return format.decodeFromString(text)
    }
}

fun simulatedVehicle(now: Long, fuel: Double = 42.0): VehicleSnapshot {
    fun metric(value: Double?, unit: String) = Metric(
        value, unit, "simulator", now,
        if (value == null) Availability.UNAVAILABLE else Availability.SIMULATED, Quality.FIXTURE,
    )
    return VehicleSnapshot(
        source = "simulator", connectionStatus = ConnectionStatus.CONNECTED, simulationMode = true,
        capturedAt = now, speedKmh = metric(0.0, "km/h"), engineRpm = metric(850.0, "rpm"),
        fuelLevelPercent = metric(fuel, "%"), coolantTemperatureC = metric(88.0, "°C"),
        batteryVoltage = metric(13.9, "V"), vehicleRangeKm = metric(null, "km"),
        supportedCapabilities = setOf("vehicle.fuel", "vehicle.temperature", "vehicle.snapshot"),
    )
}
