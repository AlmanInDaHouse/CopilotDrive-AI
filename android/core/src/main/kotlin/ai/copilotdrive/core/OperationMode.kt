package ai.copilotdrive.core

/** Independent of connection state. Hardware failure never changes this preference. */
enum class OperationMode { REAL, DEMO }

data class ModeMigration(val mode: OperationMode, val noticeRequired: Boolean)

fun migrateOperationMode(saved: String?, previouslyOnboarded: Boolean): ModeMigration =
    ModeMigration(OperationMode.entries.firstOrNull { it.name == saved } ?: OperationMode.REAL,
        saved == null && previouslyOnboarded)

enum class TelemetryState { CURRENT, DEMO, DISCONNECTED, UNSUPPORTED, STALE, UNAVAILABLE }

/** Preserve the v1 wire contract; derive capability absence separately from a missing reading. */
fun telemetryState(snapshot: VehicleSnapshot?, metric: Metric?, pid: String? = null): TelemetryState = when {
    snapshot == null || snapshot.connectionStatus == ConnectionStatus.DISCONNECTED -> TelemetryState.DISCONNECTED
    metric?.availability == Availability.STALE -> TelemetryState.STALE
    snapshot.simulationMode && metric?.availability == Availability.SIMULATED -> TelemetryState.DEMO
    !snapshot.simulationMode && pid != null && pid !in snapshot.supportedCapabilities -> TelemetryState.UNSUPPORTED
    metric?.availability == Availability.AVAILABLE -> TelemetryState.CURRENT
    else -> TelemetryState.UNAVAILABLE
}

fun telemetryLabel(snapshot: VehicleSnapshot?, metric: Metric?, pid: String? = null): String = when (telemetryState(snapshot, metric, pid)) {
    TelemetryState.CURRENT, TelemetryState.DEMO -> "${formatNumber(metric!!.value!!)} ${metric.unit}"
    TelemetryState.DISCONNECTED, TelemetryState.UNAVAILABLE -> "No disponible"
    TelemetryState.UNSUPPORTED -> "No compatible"
    TelemetryState.STALE -> "Dato obsoleto"
}
