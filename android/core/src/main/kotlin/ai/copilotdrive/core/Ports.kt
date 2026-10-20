package ai.copilotdrive.core

import kotlinx.coroutines.flow.StateFlow

fun interface VehicleDataProvider { suspend fun snapshot(): VehicleSnapshot }
class SimulatorVehicleProvider(private val clock: Clock) : VehicleDataProvider {
    override suspend fun snapshot() = simulatedVehicle(clock.now())
}
interface MediaSourceAdapter {
    val state: StateFlow<MediaSnapshot>
    suspend fun command(control: MediaControl): ToolResult
}
fun interface NavigationProvider { suspend fun openDestination(destination: String, provider: String): ToolResult }
interface ContactProvider {
    suspend fun search(name: String): List<Contact>
    suspend fun openDialer(number: String): ToolResult
}
fun interface PermissionProvider { fun status(capability: String): CapabilityStatus }
fun interface AIProvider { suspend fun generate(request: AIRequest): AIResponse }
interface SpeechToTextProvider {
    suspend fun recognize(onTranscribing: () -> Unit): String
    fun cancel()
}
interface TextToSpeechProvider {
    suspend fun speak(text: String)
    fun stop()
}
interface AudioFocusCoordinator {
    fun acquire(): Boolean
    fun release()
}
interface WakeWordDetector { val available: Boolean }

class CapabilityRegistry(private val permissions: PermissionProvider) {
    fun available(id: String) = permissions.status(id) == CapabilityStatus.AVAILABLE
    fun get(id: String) = permissions.status(id)
}

class VehicleContextEngine(private val provider: VehicleDataProvider, private val clock: Clock) {
    private var cached: VehicleSnapshot? = null
    fun invalidate() { cached = null }
    suspend fun snapshot(): VehicleSnapshot {
        val now = clock.now()
        val previous = cached
        val raw = if (previous != null && now >= previous.capturedAt && now - previous.capturedAt <= 1_000) {
            previous
        } else {
            provider.snapshot().also { cached = it }
        }
        val connected = raw.connectionStatus == ConnectionStatus.CONNECTED
        fun fresh(metric: Metric) = metric.at(now, connected)
        return raw.copy(
            speedKmh = fresh(raw.speedKmh), engineRpm = fresh(raw.engineRpm),
            fuelLevelPercent = fresh(raw.fuelLevelPercent), coolantTemperatureC = fresh(raw.coolantTemperatureC),
            batteryVoltage = fresh(raw.batteryVoltage), vehicleRangeKm = fresh(raw.vehicleRangeKm),
        )
    }
}
