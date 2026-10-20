package ai.copilotdrive.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.IOException

/** The wire vocabulary is closed. Neither UI nor model tools can supply ELM text. */
enum class ObdWireRequest(val wire: String) {
    RESET("ATZ"), ECHO_OFF("ATE0"), LINEFEEDS_OFF("ATL0"), SPACES_ON("ATS1"), AUTO_PROTOCOL("ATSP0"),
    CAPABILITIES_00("0100"), CAPABILITIES_20("0120"), RPM("010C"), SPEED("010D"),
    COOLANT("0105"), FUEL("012F"),
}

interface BluetoothTransport {
    suspend fun open(deviceAddress: String)
    suspend fun exchange(request: ObdWireRequest): String
    fun close()
}

enum class VehicleConnectionState { SIMULATED, CONNECTING, CONNECTED_REAL, DISCONNECTED, UNSUPPORTED, STALE, ERROR }

object ObdPidRegistry {
    val telemetry = listOf(ObdWireRequest.RPM, ObdWireRequest.SPEED, ObdWireRequest.COOLANT, ObdWireRequest.FUEL)
    fun supported(response: String, base: Int): Set<ObdWireRequest>? {
        val hex = ObdResponseParser.payload(response, "41%02X".format(base), 4) ?: return null
        val bits = hex.toLong(16)
        return telemetry.filter { request ->
            val pid = request.wire.drop(2).toInt(16)
            pid in (base + 1)..(base + 32) && bits and (1L shl (32 - (pid - base))) != 0L
        }.toSet()
    }
    fun hasNext(response: String, base: Int): Boolean {
        val hex = ObdResponseParser.payload(response, "41%02X".format(base), 4) ?: return false
        return hex.toLong(16) and 1L != 0L
    }
}

object ObdResponseParser {
    /** Extract one exact mode-01 frame after stripping ELM echo and prompt. */
    fun payload(response: String, header: String, bytes: Int): String? {
        if (response.contains("NO DATA", true) || response.length > 512) return null
        val lines = response.uppercase().replace('\r', '\n').replace('>', '\n').lines()
        val expected = header.chunked(2)
        for (line in lines) {
            val tokens = line.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            val frame = if (tokens.size == 1 && tokens[0].all { it in "0123456789ABCDEF" }) tokens[0].chunked(2) else tokens
            if (frame.size == expected.size + bytes && frame.take(expected.size) == expected && frame.all { it.length == 2 && it.all { c -> c in "0123456789ABCDEF" } }) {
                return frame.drop(expected.size).joinToString("")
            }
        }
        return null
    }
    fun metric(request: ObdWireRequest, response: String): Double? = when (request) {
        ObdWireRequest.RPM -> payload(response, "410C", 2)?.let { (it.take(2).toInt(16) * 256 + it.drop(2).toInt(16)) / 4.0 }
        ObdWireRequest.SPEED -> payload(response, "410D", 1)?.toInt(16)?.toDouble()
        ObdWireRequest.COOLANT -> payload(response, "4105", 1)?.toInt(16)?.minus(40)?.toDouble()
        ObdWireRequest.FUEL -> payload(response, "412F", 1)?.toInt(16)?.times(100.0)?.div(255.0)
        else -> null
    }
}

class Elm327Protocol(private val transport: BluetoothTransport) {
    suspend fun initialize() {
        for (request in listOf(ObdWireRequest.RESET, ObdWireRequest.ECHO_OFF, ObdWireRequest.LINEFEEDS_OFF,
            ObdWireRequest.SPACES_ON, ObdWireRequest.AUTO_PROTOCOL)) {
            val response = transport.exchange(request)
            if (response.contains("?")) throw IOException("ELM initialization rejected")
        }
    }
}

class VehicleCapabilityDiscovery(private val transport: BluetoothTransport) {
    suspend fun discover(): Set<ObdWireRequest> {
        val first = transport.exchange(ObdWireRequest.CAPABILITIES_00)
        val supported = ObdPidRegistry.supported(first, 0) ?: throw IOException("Invalid capabilities response")
        if (!ObdPidRegistry.hasNext(first, 0)) return supported
        val next = transport.exchange(ObdWireRequest.CAPABILITIES_20)
        return supported + (ObdPidRegistry.supported(next, 0x20) ?: throw IOException("Invalid capabilities response"))
    }
}

class TelemetryPollingCoordinator(
    private val transport: BluetoothTransport, private val clock: Clock,
    private val supported: Set<ObdWireRequest>, private val onReading: (ObdWireRequest, Double?, Long) -> Unit,
) {
    suspend fun pollOnce() {
        for (request in ObdPidRegistry.telemetry.filter { it in supported }) {
            val raw = withTimeout(3_000) { transport.exchange(request) }
            onReading(request, ObdResponseParser.metric(request, raw), clock.now())
        }
    }
}

class BluetoothObdProvider(
    private val transport: BluetoothTransport, private val clock: Clock, private val scope: CoroutineScope,
    private val onDiagnostic: (DiagnosticCheck) -> Unit = {},
) : VehicleDataProvider {
    private fun observe(id: String, status: DiagnosticStatus, startedNs: Long, code: String? = null) {
        onDiagnostic(DiagnosticCheck("obd.$id", "bluetooth", status, EvidenceLevel.PENDING, clock.now(),
            ((System.nanoTime() - startedNs) / 1_000_000).coerceAtLeast(0), code,
            "OBD $id: ${status.name}; verificar hardware y vehículo por separado.",
            if (status == DiagnosticStatus.FAIL) "Revisa emparejamiento, permisos, alimentación y compatibilidad ELM." else null))
    }
    private val mutableState = MutableStateFlow(VehicleConnectionState.SIMULATED)
    val state: StateFlow<VehicleConnectionState> = mutableState
    private val mutableSnapshot = MutableStateFlow(simulatedVehicle(clock.now()))
    val readings: StateFlow<VehicleSnapshot> = mutableSnapshot
    private val mutableCapabilities = MutableStateFlow<Set<ObdWireRequest>>(emptySet())
    val capabilities: StateFlow<Set<ObdWireRequest>> = mutableCapabilities
    private val mutableError = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = mutableError
    private var session: Job? = null

    override suspend fun snapshot(): VehicleSnapshot {
        if (mutableState.value == VehicleConnectionState.SIMULATED) {
            val current = simulatedVehicle(clock.now())
            mutableSnapshot.value = current
            return current
        }
        return mutableSnapshot.value
    }

    fun connect(address: String) {
        val connectionStart = System.nanoTime()
        val previous = session
        previous?.cancel()
        transport.close()
        mutableCapabilities.value = emptySet()
        mutableSnapshot.value = disconnectedSnapshot(clock.now())
        mutableState.value = VehicleConnectionState.CONNECTING
        mutableError.value = null
        session = scope.launch {
            try {
                previous?.join()
                var attempts = 0
                while (attempts < 2) {
                    try {
                        val openStart = System.nanoTime()
                        withTimeout(8_000) { transport.open(address) }
                        observe("rfcomm", DiagnosticStatus.PASS, openStart)
                        val initStart = System.nanoTime()
                        withTimeout(8_000) { Elm327Protocol(transport).initialize() }
                        observe("elm_init", DiagnosticStatus.PASS, initStart)
                        val discoveryStart = System.nanoTime()
                        val supported = withTimeout(5_000) { VehicleCapabilityDiscovery(transport).discover() }
                        observe("pid_discovery", DiagnosticStatus.PASS, discoveryStart)
                        mutableCapabilities.value = supported
                        mutableSnapshot.value = emptyRealSnapshot(clock.now(), supported)
                        mutableState.value = VehicleConnectionState.CONNECTED_REAL
                        observe("connection", DiagnosticStatus.PASS, connectionStart)
                        val poller = TelemetryPollingCoordinator(transport, clock, supported) { request, value, at ->
                            mutableSnapshot.value = mutableSnapshot.value.withReading(request, value, at)
                            observe(if (value == null) "reading_unavailable" else "reading_success", DiagnosticStatus.PASS, System.nanoTime(),
                                request.wire)
                        }
                        while (true) { poller.pollOnce(); delay(1_000) }
                    } catch (timeout: TimeoutCancellationException) {
                        observe("timeout", DiagnosticStatus.FAIL, connectionStart, "TIMEOUT")
                        transport.close()
                        mutableSnapshot.value = disconnectedSnapshot(clock.now())
                        attempts++
                        if (attempts == 2) {
                            mutableError.value = "Tiempo de espera OBD agotado"
                            mutableState.value = VehicleConnectionState.ERROR
                            mutableSnapshot.value = disconnectedSnapshot(clock.now())
                        } else { mutableState.value = VehicleConnectionState.STALE; observe("reconnect", DiagnosticStatus.PENDING, connectionStart); delay(1_000) }
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (failure: Exception) {
                        observe("disconnect", DiagnosticStatus.FAIL, connectionStart, failure.javaClass.simpleName)
                        transport.close()
                        mutableSnapshot.value = disconnectedSnapshot(clock.now())
                        attempts++
                        if (attempts == 2) {
                            mutableError.value = failure.message ?: "Error de conexión OBD"
                            mutableState.value = VehicleConnectionState.ERROR
                            mutableSnapshot.value = disconnectedSnapshot(clock.now())
                        } else { mutableState.value = VehicleConnectionState.STALE; observe("reconnect", DiagnosticStatus.PENDING, connectionStart); delay(1_000) }
                    }
                }
            } finally { transport.close(); observe("socket_close", DiagnosticStatus.PENDING, System.nanoTime()) }
        }
    }

    fun disconnect(simulator: Boolean = false) {
        session?.cancel(); session = null; transport.close()
        mutableCapabilities.value = emptySet()
        mutableState.value = if (simulator) VehicleConnectionState.SIMULATED else VehicleConnectionState.DISCONNECTED
        mutableSnapshot.value = if (simulator) simulatedVehicle(clock.now()) else disconnectedSnapshot(clock.now())
        observe(if (simulator) "simulator_return" else "manual_disconnect", DiagnosticStatus.PASS, System.nanoTime())
    }

    fun unsupported(reason: String) {
        disconnect()
        mutableError.value = reason
        mutableState.value = VehicleConnectionState.UNSUPPORTED
    }

    private fun disconnectedSnapshot(now: Long) = emptyRealSnapshot(now, emptySet()).copy(connectionStatus = ConnectionStatus.DISCONNECTED)
}

private fun emptyRealSnapshot(now: Long, supported: Set<ObdWireRequest>): VehicleSnapshot {
    fun metric(unit: String) = Metric(null, unit, "obd", now, Availability.UNAVAILABLE)
    return VehicleSnapshot(source = "obd", connectionStatus = ConnectionStatus.CONNECTED, simulationMode = false,
        capturedAt = now, speedKmh = metric("km/h"), engineRpm = metric("rpm"), fuelLevelPercent = metric("%"),
        coolantTemperatureC = metric("°C"), batteryVoltage = metric("V"), vehicleRangeKm = metric("km"),
        supportedCapabilities = supported.map { it.wire }.toSet())
}

private fun VehicleSnapshot.withReading(request: ObdWireRequest, value: Double?, at: Long): VehicleSnapshot {
    val old = when (request) {
        ObdWireRequest.RPM -> engineRpm; ObdWireRequest.SPEED -> speedKmh
        ObdWireRequest.COOLANT -> coolantTemperatureC; ObdWireRequest.FUEL -> fuelLevelPercent
        else -> return this
    }
    val metric = old.copy(value = value, capturedAt = at,
        availability = if (value == null) Availability.UNAVAILABLE else Availability.AVAILABLE,
        quality = if (value == null) Quality.UNKNOWN else Quality.SENSOR_REPORTED)
    return when (request) {
        ObdWireRequest.RPM -> copy(engineRpm = metric, capturedAt = at)
        ObdWireRequest.SPEED -> copy(speedKmh = metric, capturedAt = at)
        ObdWireRequest.COOLANT -> copy(coolantTemperatureC = metric, capturedAt = at)
        ObdWireRequest.FUEL -> copy(fuelLevelPercent = metric, capturedAt = at)
        else -> this
    }
}
