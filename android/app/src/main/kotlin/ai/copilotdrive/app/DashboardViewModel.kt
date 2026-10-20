package ai.copilotdrive.app

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ai.copilotdrive.app.platform.*
import ai.copilotdrive.core.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardState(
    val vehicle: VehicleSnapshot? = null,
    val media: MediaSnapshot = MediaSnapshot(),
    val artwork: Bitmap? = null,
    val voice: VoiceSessionState = VoiceSessionState(),
    val preferences: VoicePreferences = VoicePreferences(),
    val preferencesLoaded: Boolean = false,
    val voices: List<InstalledVoice> = emptyList(),
    val capabilities: List<Capability> = emptyList(),
    val sessions: List<MediaSessionChoice> = emptyList(),
    val demoMedia: Boolean = true,
    val clock: String = "--:--",
    val destination: String? = null,
    val notice: String = "Datos ficticios · Movimiento del vehículo desconocido",
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val mutable = MutableStateFlow(DashboardState())
    val state = mutable.asStateFlow()
    private val preferences = Preferences(app)
    private val clock = ai.copilotdrive.core.SystemClock
    private val vehicle = VehicleContextEngine(SimulatorVehicleProvider(clock), clock)
    val androidMedia = AndroidMediaAdapter(app)
    private val demoMediaAdapter = DemoMediaAdapter(clock)
    private var demoMedia = true
    private val media = object : MediaSourceAdapter {
        override val state = MutableStateFlow(demoMediaAdapter.state.value)
        override suspend fun command(control: MediaControl): ToolResult {
            val result = (if (demoMedia) demoMediaAdapter else androidMedia).command(control)
            refreshMedia()
            return result
        }
    }
    private val androidNavigation = AndroidNavigation(app)
    private val contacts = AndroidContacts(app)
    private val navigation = NavigationProvider { destination, provider ->
        androidNavigation.openDestination(destination, provider).also { result ->
            if (result.executionStatus == ExecutionStatus.SUCCESS) mutable.update { it.copy(destination = destination) }
        }
    }
    private val stt = AndroidSpeechRecognizer(app) { mutable.value.preferences.allowSystemRecognition }
    private val tts = AndroidTextToSpeech(app)
    private val permissionProvider = PermissionProvider { id ->
        when (id) {
            "vehicle.fuel", "vehicle.temperature", "vehicle.snapshot" -> CapabilityStatus.AVAILABLE
            "vehicle.obd_connected" -> CapabilityStatus.NOT_CONFIGURED
            "microphone.available" -> if (app.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) CapabilityStatus.AVAILABLE else CapabilityStatus.PERMISSION_REQUIRED
            "voice.recognition" -> when { !stt.available() -> CapabilityStatus.UNSUPPORTED; !stt.localAvailable() && !mutable.value.preferences.allowSystemRecognition -> CapabilityStatus.NOT_CONFIGURED; else -> CapabilityStatus.AVAILABLE }
            "voice.wakeword_available" -> CapabilityStatus.NOT_CONFIGURED
            "tts.available" -> if (tts.voices.value.isNotEmpty()) CapabilityStatus.AVAILABLE else CapabilityStatus.NOT_CONFIGURED
            "media.session_access", "media.playback_control" -> if (demoMedia || androidMedia.authorized()) CapabilityStatus.AVAILABLE else CapabilityStatus.PERMISSION_REQUIRED
            "navigation.available" -> if (androidNavigation.available("maps") || androidNavigation.available("waze")) CapabilityStatus.AVAILABLE else CapabilityStatus.UNSUPPORTED
            "navigation.google_maps" -> if (androidNavigation.available("maps")) CapabilityStatus.AVAILABLE else CapabilityStatus.UNSUPPORTED
            "navigation.waze" -> if (androidNavigation.available("waze")) CapabilityStatus.AVAILABLE else CapabilityStatus.UNSUPPORTED
            "contacts.access" -> if (contacts.permitted()) CapabilityStatus.AVAILABLE else CapabilityStatus.PERMISSION_REQUIRED
            "phone.dialer" -> if (contacts.dialerAvailable()) CapabilityStatus.AVAILABLE else CapabilityStatus.UNSUPPORTED
            else -> CapabilityStatus.NOT_CONFIGURED
        }
    }
    private val capabilities = CapabilityRegistry(permissionProvider)
    private val tools = ToolRegistry(PolicyEngine(capabilities), vehicle, media, navigation, contacts)
    private val assistant = AssistantOrchestrator(tools, clock)
    private val focus: AndroidAudioFocus = AndroidAudioFocus(app) { cancel() }
    private val voice: VoiceSessionCoordinator = VoiceSessionCoordinator(viewModelScope, stt, tts, assistant, focus, clock) { capabilities.available("microphone.available") }
    private var ticker: Job? = null

    init {
        viewModelScope.launch {
            preferences.state.catch { mutable.update { it.copy(preferencesLoaded = true, notice = "No se pudieron cargar las preferencias.") } }
                .collect { prefs ->
                    mutable.update { it.copy(preferences = prefs, preferencesLoaded = true) }
                    tts.selectedVoice = prefs.voice; tts.rate = prefs.rate; tts.pitch = prefs.pitch
                    refreshCapabilities()
                }
        }
        viewModelScope.launch { tts.initialize(); mutable.update { it.copy(voices = tts.voices.value) }; refreshCapabilities() }
        viewModelScope.launch { voice.state.collect { session -> mutable.update { it.copy(voice = session) }; refreshMedia() } }
        viewModelScope.launch { androidMedia.state.collect { refreshMedia() } }
    }

    fun foreground() {
        if (ticker?.isActive == true) return
        ticker = viewModelScope.launch {
            while (true) {
                androidMedia.refresh()
                refreshMedia(); refreshCapabilities()
                val snapshot = vehicle.snapshot()
                mutable.update { it.copy(vehicle = snapshot, clock = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
                delay(1_000)
            }
        }
    }
    fun background() { ticker?.cancel(); ticker = null; cancel(); androidMedia.close() }
    fun listen() { if (mutable.value.preferences.onboarded) voice.listen() }
    fun demo(text: String) { if (mutable.value.preferences.onboarded) voice.demo(text) }
    fun cancel() { voice.cancel() }
    fun savePreferences(value: VoicePreferences) {
        if (mutable.value.preferences.onboarded) cancel()
        viewModelScope.launch {
            try { preferences.save(value) } catch (_: java.io.IOException) { mutable.update { it.copy(notice = "No se pudieron guardar las preferencias.") } }
        }
    }
    fun setDemoMedia(enabled: Boolean) { demoMedia = enabled; androidMedia.refresh(); refreshMedia(); refreshCapabilities() }
    fun mediaControl(control: MediaControl) {
        viewModelScope.launch {
            val result = tools.execute(ToolRequest("media.${control.name.lowercase()}"))
            mutable.update { it.copy(notice = result.message) }
            refreshMedia()
        }
    }
    private fun refreshMedia() {
        media.state.value = if (demoMedia) demoMediaAdapter.state.value else androidMedia.state.value
        mutable.update { it.copy(media = media.state.value, artwork = if (demoMedia) null else androidMedia.artwork.value, sessions = androidMedia.sessions.value, demoMedia = demoMedia) }
    }
    private fun refreshCapabilities() {
        val labels = mapOf(
            "microphone.available" to "Micrófono", "voice.recognition" to "Reconocimiento de voz", "tts.available" to "Voces instaladas",
            "voice.wakeword_available" to "Wake word", "media.session_access" to if (demoMedia) "Audio simulado" else "Sesiones Android",
            "vehicle.obd_connected" to "Adaptador OBD", "navigation.google_maps" to "Google Maps", "navigation.waze" to "Waze",
            "contacts.access" to "Contactos", "phone.dialer" to "Marcador", "places.provider_configured" to "Lugares cercanos",
        )
        mutable.update { it.copy(capabilities = labels.map { (id, label) -> Capability(id, capabilities.get(id), label) }) }
    }
    override fun onCleared() { background(); tts.close(); super.onCleared() }
}
