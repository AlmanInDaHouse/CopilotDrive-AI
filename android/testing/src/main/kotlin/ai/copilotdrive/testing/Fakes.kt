package ai.copilotdrive.testing

import ai.copilotdrive.core.*
import kotlinx.coroutines.delay

class DeterministicClock(var time: Long = 1_700_000_000_000) : Clock {
    override fun now() = time
    fun advance(ms: Long) { time += ms }
}

class FakeVehicleProvider(var value: VehicleSnapshot) : VehicleDataProvider {
    var calls = 0
    var failure: Exception? = null
    var delayMs = 0L
    override suspend fun snapshot(): VehicleSnapshot {
        calls++
        delay(delayMs)
        failure?.let { throw it }
        return value
    }
}

class FakeMediaAdapter(clock: Clock) : MediaSourceAdapter {
    private val demo = DemoMediaAdapter(clock)
    override val state = demo.state
    val calls = mutableListOf<MediaControl>()
    var reject = false
    override suspend fun command(control: MediaControl): ToolResult {
        if (reject || control !in state.value.supportedControls) return ToolResult.denied("media", "UNSUPPORTED", "Control no disponible.")
        calls += control
        return demo.command(control)
    }
}

class FakePermissionProvider : PermissionProvider {
    val values = mutableMapOf<String, CapabilityStatus>()
    override fun status(capability: String) = values[capability] ?: CapabilityStatus.NOT_CONFIGURED
    fun grant(vararg capabilities: String) = capabilities.forEach { values[it] = CapabilityStatus.AVAILABLE }
}

class FakeNavigationProvider : NavigationProvider {
    val calls = mutableListOf<Pair<String, String>>()
    var available = true
    override suspend fun openDestination(destination: String, provider: String): ToolResult {
        if (!available) return ToolResult.denied("navigation.open_destination", "NO_HANDLER", "No hay navegador compatible.")
        calls += destination to provider
        return ToolResult("navigation.open_destination", ExecutionStatus.SUCCESS, "Simulación: apertura de navegador.")
    }
}

class FakeContactsProvider : ContactProvider {
    var contacts = listOf(Contact("demo-1", "Mercedes Demo", "+34900000000"))
    val dialed = mutableListOf<String>()
    override suspend fun search(name: String) = contacts.filter { normalize(it.name).contains(normalize(name)) }
    override suspend fun openDialer(number: String): ToolResult {
        dialed += number
        return ToolResult("phone.open_dialer", ExecutionStatus.SUCCESS, "Simulación: marcador abierto; no se ha llamado.")
    }
}

class FakePlacesProvider : PlacesProvider {
    override suspend fun searchNearby(query: String) = listOf(Place("Lugar ficticio de prueba", 4.2, 1.0, true))
}

class FakeAIProvider(var proposal: ToolRequest? = null) : AIProvider {
    var calls = 0
    override suspend fun propose(request: AssistantRequest): ToolRequest? { calls++; return proposal }
}

class FakeSpeechRecognizer : SpeechToTextProvider {
    var text = "Jarvis, cuánta gasolina queda"
    var calls = 0
    var delayMs = 0L
    var failure: Exception? = null
    override suspend fun recognize(onTranscribing: () -> Unit): String {
        calls++
        delay(delayMs)
        failure?.let { throw it }
        onTranscribing()
        return text
    }
    override fun cancel() = Unit
}

class FakeTextToSpeech : TextToSpeechProvider {
    val spoken = mutableListOf<String>()
    var failure: Exception? = null
    override suspend fun speak(text: String) { failure?.let { throw it }; spoken += text }
    override fun stop() = Unit
}

class FakeAudioFocus : AudioFocusCoordinator {
    var granted = true
    var held = false
    override fun acquire(): Boolean { held = granted; return granted }
    override fun release() { held = false }
}

class TestHarness(val clock: DeterministicClock = DeterministicClock()) {
    val vehicle = FakeVehicleProvider(simulatedVehicle(clock.now()))
    val media = FakeMediaAdapter(clock)
    val permissions = FakePermissionProvider().apply {
        grant("vehicle.fuel", "vehicle.temperature", "vehicle.snapshot", "media.session_access", "media.playback_control",
            "navigation.available", "contacts.access", "phone.dialer", "microphone.available")
    }
    val navigation = FakeNavigationProvider()
    val contacts = FakeContactsProvider()
    val context = VehicleContextEngine(vehicle, clock)
    val tools = ToolRegistry(PolicyEngine(CapabilityRegistry(permissions)), context, media, navigation, contacts)
    val ai = FakeAIProvider()
    val assistant = AssistantOrchestrator(tools, clock, ai)
    val stt = FakeSpeechRecognizer()
    val tts = FakeTextToSpeech()
    val focus = FakeAudioFocus()
    suspend fun ask(text: String) = assistant.answer(AssistantRequest("test", "test", text, InputMode.VOICE, clock.now()))
}
