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
    var results = listOf(PlaceSearchResult("demo-1", "Lugar ficticio de prueba", PlaceCategory.RESTAURANT,
        "Dirección de demostración", Coordinates(40.4168, -3.7038), 4.2, 12, null, null,
        "FAKE · SIMULACIÓN", 1_700_000_000_000, Availability.SIMULATED))
    var error: PlacesProviderError? = null
    var calls = 0
    var delayMs = 0L
    var onSearch: (() -> Unit)? = null
    override suspend fun searchNearby(request: PlaceSearchRequest): List<PlaceSearchResult> {
        calls++
        onSearch?.invoke()
        delay(delayMs)
        error?.let { throw it }
        return results.map { it.copy(category = request.category, capturedAt = request.center.capturedAt) }
    }
}

class FakeLocationProvider(var snapshot: LocationSnapshot = LocationSnapshot(Coordinates(40.4168, -3.7038),
    500f, 1_700_000_000_000, "fake", LocationAvailability.AVAILABLE)) : LocationProvider {
    var calls = 0
    override suspend fun current(): LocationSnapshot { calls++; return snapshot }
}

class FakeAIProvider(var proposal: ToolRequest? = null) : AIProvider {
    var calls = 0
    var response: AIResponse? = null
    var lastRequest: AIRequest? = null
    override suspend fun generate(request: AIRequest): AIResponse {
        calls++; lastRequest = request
        return response ?: proposal?.let { AIResponse.ProposedTool(ToolProposal(it.toolId, it.arguments)) }
            ?: AIResponse.Unavailable(AIError("NOT_CONFIGURED", "IA no configurada."))
    }
}

class FakeConversationStore : ConversationStore by EphemeralConversationStore()
class FakeProviderSettings(var value: ProviderConfiguration = ProviderConfiguration()) : ProviderSettings {
    override fun current() = value
}
class FakeSecretStore(var value: String? = null) : SecretStore {
    override fun get() = value
    override fun put(secret: String) { value = secret }
    override fun clear() { value = null }
}
class FakeHTTPTransport(var response: HTTPResponse = HTTPResponse(200, "")) : HTTPTransport {
    var calls = 0
    var lastUrl: String? = null
    var lastBody: String? = null
    var failure: Exception? = null
    var delayMs = 0L
    override suspend fun post(url: String, token: String, body: String, timeoutMs: Int): HTTPResponse {
        calls++; lastUrl = url; lastBody = body
        delay(delayMs)
        failure?.let { throw it }
        return response
    }
}
class FakeTTSVoiceCatalog(var voices: List<String> = emptyList())

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
    var delayMs = 0L
    override suspend fun speak(text: String) { delay(delayMs); failure?.let { throw it }; spoken += text }
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
