package ai.copilotdrive.app

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.SystemClock
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
    val vehicleConnection: VehicleConnectionState = VehicleConnectionState.SIMULATED,
    val obdDevices: List<PairedObdDevice> = emptyList(),
    val selectedObdAddress: String? = null,
    val obdCapabilities: Set<String> = emptySet(),
    val obdError: String? = null,
    val media: MediaSnapshot = MediaSnapshot(),
    val artwork: Bitmap? = null,
    val voice: VoiceSessionState = VoiceSessionState(),
    val preferences: VoicePreferences = VoicePreferences(),
    val preferencesLoaded: Boolean = false,
    val voices: List<InstalledVoice> = emptyList(),
    val credentialSaved: Boolean = false,
    val ttsStatus: String = "Inicializando voz…",
    val capabilities: List<Capability> = emptyList(),
    val sessions: List<MediaSessionChoice> = emptyList(),
    val demoMedia: Boolean = true,
    val clock: String = "--:--",
    val destination: String? = null,
    val places: PlacesState = PlacesState(),
    val placesConsent: Boolean = false,
    val placesStationary: Boolean = false,
    val placesConfigured: Boolean = BuildConfig.PLACES_ANDROID_API_KEY.isNotBlank(),
    val notice: String = "Datos ficticios · Movimiento del vehículo desconocido",
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val mutable = MutableStateFlow(DashboardState())
    val state = mutable.asStateFlow()
    private val preferences = Preferences(app)
    private val secrets = KeystoreSecretStore(app)
    private val clock = ai.copilotdrive.core.SystemClock
    private val bluetooth = AndroidBluetoothObd(app)
    private val diagnosticHistory = DiagnosticHistory()
    private val obd = BluetoothObdProvider(bluetooth, clock, viewModelScope, diagnosticHistory::record)
    private val vehicle = VehicleContextEngine(obd, clock)
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
    private val androidLocation = AndroidLocationProvider(app)
    private var demoPlaces = false
    private var placesConsent = false
    private var stationaryPlaces = false
    private var stationaryConfirmedAt = 0L
    private fun stationaryValid() = stationaryPlaces && clock.now() >= stationaryConfirmedAt && clock.now() - stationaryConfirmedAt <= 120_000
    private val contacts = AndroidContacts(app)
    private val navigation = NavigationProvider { destination, provider ->
        androidNavigation.openDestination(destination, provider).also { result ->
            if (result.executionStatus == ExecutionStatus.SUCCESS) mutable.update { it.copy(destination = destination) }
        }
    }
    private val places = PlacesController(
        object : LocationProvider {
            override suspend fun current(): LocationSnapshot = if (demoPlaces)
                LocationSnapshot(Coordinates(40.4168, -3.7038), 500f, clock.now(), "SIMULACIÓN · Madrid", LocationAvailability.AVAILABLE)
            else androidLocation.current()
        },
        object : PlacesProvider {
            override suspend fun searchNearby(request: PlaceSearchRequest): List<PlaceSearchResult> =
                if (demoPlaces) DemoPlacesProvider(clock).searchNearby(request)
                else GooglePlacesProvider(app, BuildConfig.PLACES_ANDROID_API_KEY, clock).searchNearby(request)
        },
        NavigationProvider { destination, provider -> if (demoPlaces)
            ToolResult("navigation.open_selected_place", ExecutionStatus.SUCCESS, "Simulación: destino enviado al navegador ficticio. No hay ruta activa.")
            else navigation.openDestination(destination, provider) }, clock,
        canQuery = { demoPlaces || placesConsent },
        hasLocationPermission = { demoPlaces || androidLocation.permitted() },
        isDemo = { demoPlaces },
        safeToExplore = { stationaryValid() },
    )
    private val latencyTracker = VoiceLatencyTracker()
    val voiceLatencies = latencyTracker.summaries
    private val stt = AndroidSpeechRecognizer(app, { mutable.value.preferences.allowSystemRecognition }) {
        recordVoiceMetric(VoiceMetric.STT_INIT, it)
    }
    private val tts = AndroidTextToSpeech(app)
    val diagnostics = diagnosticHistory.checks
    private val deviceDiagnostics = DeviceDiagnostics(app, bluetooth, stt, tts)
    private var obdStartedMs: Long? = null
    private val permissionProvider = PermissionProvider { id ->
        when (id) {
            "vehicle.fuel", "vehicle.temperature", "vehicle.snapshot" -> CapabilityStatus.AVAILABLE
            "vehicle.obd_connected" -> when {
                !bluetooth.present() || obd.state.value == VehicleConnectionState.UNSUPPORTED -> CapabilityStatus.UNSUPPORTED
                !bluetooth.permitted() -> CapabilityStatus.PERMISSION_REQUIRED
                obd.state.value == VehicleConnectionState.CONNECTED_REAL -> CapabilityStatus.AVAILABLE
                obd.state.value == VehicleConnectionState.ERROR -> CapabilityStatus.ERROR
                else -> CapabilityStatus.DISCONNECTED
            }
            "microphone.available" -> if (app.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) CapabilityStatus.AVAILABLE else CapabilityStatus.PERMISSION_REQUIRED
            "voice.recognition" -> when { !stt.available() -> CapabilityStatus.UNSUPPORTED; !stt.localAvailable() && !mutable.value.preferences.allowSystemRecognition -> CapabilityStatus.NOT_CONFIGURED; else -> CapabilityStatus.AVAILABLE }
            "voice.wakeword_available" -> CapabilityStatus.NOT_CONFIGURED
            "tts.available" -> if (tts.voices.value.isNotEmpty()) CapabilityStatus.AVAILABLE else CapabilityStatus.NOT_CONFIGURED
            "media.session_access", "media.playback_control" -> if (demoMedia || androidMedia.authorized()) CapabilityStatus.AVAILABLE else CapabilityStatus.PERMISSION_REQUIRED
            "navigation.available" -> if (demoPlaces || androidNavigation.available("maps") || androidNavigation.available("waze")) CapabilityStatus.AVAILABLE else CapabilityStatus.UNSUPPORTED
            "navigation.google_maps" -> if (androidNavigation.available("maps")) CapabilityStatus.AVAILABLE else CapabilityStatus.UNSUPPORTED
            "navigation.waze" -> if (androidNavigation.available("waze")) CapabilityStatus.AVAILABLE else CapabilityStatus.UNSUPPORTED
            "contacts.access" -> if (contacts.permitted()) CapabilityStatus.AVAILABLE else CapabilityStatus.PERMISSION_REQUIRED
            "phone.dialer" -> if (contacts.dialerAvailable()) CapabilityStatus.AVAILABLE else CapabilityStatus.UNSUPPORTED
            "places.available", "places.provider_configured" -> if (demoPlaces || BuildConfig.PLACES_ANDROID_API_KEY.isNotBlank()) CapabilityStatus.AVAILABLE else CapabilityStatus.NOT_CONFIGURED
            else -> CapabilityStatus.NOT_CONFIGURED
        }
    }
    private val capabilities = CapabilityRegistry(permissionProvider)
    private val tools = ToolRegistry(PolicyEngine(capabilities), vehicle, media, navigation, contacts, places)
    private val aiProvider = OpenAICompatibleProvider(
        { mutable.value.preferences.ai }, { secrets.get() }, AndroidHTTPTransport(),
        allowLocalHttp = (app.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0,
    )
    private val assistant = AssistantOrchestrator(tools, clock, aiProvider)
    private val focus: AndroidAudioFocus = AndroidAudioFocus(app) { cancel() }
    private val voice: VoiceSessionCoordinator = VoiceSessionCoordinator(viewModelScope, stt, tts, assistant, focus, clock,
        ::recordVoiceMetric, ::recordVoiceFailure, { capabilities.available("microphone.available") })
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
        viewModelScope.launch {
            val started = SystemClock.elapsedRealtime()
            tts.initialize()
            recordVoiceMetric(VoiceMetric.TTS_INIT, (SystemClock.elapsedRealtime() - started).coerceAtLeast(0))
            mutable.update { it.copy(voices = tts.voices.value, ttsStatus = tts.status.value, credentialSaved = secrets.get() != null) }
            refreshCapabilities()
        }
        viewModelScope.launch { voice.state.collect { session -> mutable.update { it.copy(voice = session) }; refreshMedia() } }
        viewModelScope.launch { androidMedia.state.collect { refreshMedia() } }
        viewModelScope.launch { obd.state.collect { connection ->
            vehicle.invalidate()
            mutable.update { it.copy(vehicleConnection = connection, obdError = obd.error.value) }
            refreshCapabilities()
            if (connection == VehicleConnectionState.CONNECTED_REAL || connection == VehicleConnectionState.ERROR ||
                connection == VehicleConnectionState.STALE || connection == VehicleConnectionState.DISCONNECTED) {
                val started = obdStartedMs
                if (started != null) {
                    diagnosticHistory.record(DiagnosticCheck("obd.connection", "bluetooth",
                        if (connection == VehicleConnectionState.CONNECTED_REAL) DiagnosticStatus.PASS else DiagnosticStatus.FAIL,
                        EvidenceLevel.PENDING, System.currentTimeMillis(),
                        (SystemClock.elapsedRealtime() - started).coerceAtLeast(0),
                        if (connection == VehicleConnectionState.CONNECTED_REAL) null else connection.name,
                        "Estado RFCOMM/ELM: ${connection.name}; comprobación física manual pendiente.",
                        if (connection == VehicleConnectionState.CONNECTED_REAL) null else "Revisa alimentación, emparejamiento y permisos."))
                    if (connection != VehicleConnectionState.STALE) obdStartedMs = null
                }
            }
        } }
        viewModelScope.launch { obd.capabilities.collect { supported ->
            mutable.update { it.copy(obdCapabilities = supported.map { pid -> pid.wire }.toSet()) }
        } }
        viewModelScope.launch { places.state.collect { current -> mutable.update { it.copy(places = current) } } }
    }

    fun foreground() {
        if (ticker?.isActive == true) return
        viewModelScope.launch {
            tts.initialize()
            mutable.update { it.copy(voices = tts.voices.value, ttsStatus = tts.status.value) }
            refreshCapabilities()
        }
        ticker = viewModelScope.launch {
            while (true) {
                if (stationaryPlaces && !stationaryValid()) setPlacesStationary(false)
                androidMedia.refresh()
                refreshMedia(); refreshCapabilities()
                val snapshot = vehicle.snapshot()
                mutable.update { it.copy(vehicle = snapshot, clock = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
                delay(1_000)
            }
        }
    }
    fun background() { ticker?.cancel(); ticker = null; setPlacesStationary(false); cancel(); androidMedia.close(); if (obd.state.value != VehicleConnectionState.SIMULATED) obd.disconnect() }
    fun refreshPairedBluetooth() {
        try {
            if (!bluetooth.present()) {
                obd.unsupported("Este dispositivo no dispone de Bluetooth.")
                mutable.update { it.copy(obdDevices = emptyList(), obdError = "Este dispositivo no dispone de Bluetooth.") }
                return
            }
            val devices = bluetooth.pairedDevices()
            mutable.update { it.copy(obdDevices = devices, obdError = when {
                !bluetooth.available() -> "Bluetooth no disponible o apagado."
                devices.isEmpty() -> "No hay adaptadores emparejados. Empareja uno en Ajustes de Android."
                else -> null
            }) }
        } catch (_: SecurityException) { mutable.update { it.copy(obdDevices = emptyList(), selectedObdAddress = null,
            obdError = "Autoriza dispositivos cercanos para consultar los emparejados.") } }
    }
    fun selectObd(address: String) {
        if (mutable.value.obdDevices.none { it.address == address }) return
        mutable.update { it.copy(selectedObdAddress = address) }
    }
    fun connectObd() {
        val address = mutable.value.selectedObdAddress ?: return
        if (!bluetooth.present()) { obd.unsupported("Este dispositivo no dispone de Bluetooth."); return }
        if (!bluetooth.permitted()) { mutable.update { it.copy(obdError = "Falta permiso Bluetooth.") }; return }
        if (!bluetooth.available()) { mutable.update { it.copy(obdError = "Bluetooth no disponible o apagado.") }; return }
        obdStartedMs = SystemClock.elapsedRealtime()
        vehicle.invalidate(); obd.connect(address)
    }
    fun disconnectObd() { obd.disconnect(); vehicle.invalidate() }
    fun useSimulator() { obd.disconnect(true); vehicle.invalidate() }
    fun runLocalDiagnostics() {
        val current = mutable.value
        deviceDiagnostics.local(current.preferences.ai, current.credentialSaved,
            if (current.vehicleConnection == VehicleConnectionState.SIMULATED) "SIMULATED" else "REAL",
            current.obdCapabilities).forEach(diagnosticHistory::record)
        val now = clock.now()
        val locationAllowed = androidLocation.permitted()
        diagnosticHistory.record(DiagnosticCheck("places.location_permission", "places", if (locationAllowed) DiagnosticStatus.PASS else DiagnosticStatus.PENDING,
            EvidenceLevel.PENDING, now, description = if (locationAllowed) "Ubicación aproximada o precisa autorizada; GPS no verificado." else "Permiso de ubicación pendiente o denegado."))
        current.places.location?.let { fix -> diagnosticHistory.record(DiagnosticCheck("places.location_source", "places",
            if (fix.availability == LocationAvailability.AVAILABLE) DiagnosticStatus.PASS else DiagnosticStatus.PENDING,
            if (demoPlaces) EvidenceLevel.FAKE else EvidenceLevel.PENDING, now,
            description = "Fuente ${fix.source.take(40)}; precisión informada ${fix.accuracyMeters?.toInt()?.toString() ?: "desconocida"} m; estado ${fix.availability.name}. Sin coordenadas.")) }
        diagnosticHistory.record(DiagnosticCheck("places.provider", "places", if (demoPlaces || current.placesConfigured) DiagnosticStatus.PASS else DiagnosticStatus.PENDING,
            EvidenceLevel.PENDING, now, description = if (demoPlaces) "Proveedor de simulación activo; sin consulta externa." else if (current.placesConfigured)
                "Google Places configurado; respuesta real sin verificar." else "Google Places no configurado."))
        diagnosticHistory.record(DiagnosticCheck("places.consent", "places", if (placesConsent) DiagnosticStatus.PASS else DiagnosticStatus.PENDING,
            EvidenceLevel.PENDING, now, description = if (placesConsent) "Consulta externa autorizada para esta sesión." else "Sin autorización de Places."))
        listOf("maps", "waze").forEach { name -> diagnosticHistory.record(DiagnosticCheck("navigation.$name", "navigation",
            if (androidNavigation.available(name)) DiagnosticStatus.PASS else DiagnosticStatus.PENDING, EvidenceLevel.PENDING, now,
            description = "$name ${if (androidNavigation.available(name)) "resuelve un intent" else "no disponible"}; apertura real sin verificar.")) }
    }
    fun checkBluetoothDiagnostics() {
        runLocalDiagnostics()
        diagnosticHistory.record(deviceDiagnostics.bluetoothPairing())
    }
    fun checkVoiceDiagnostics() { runLocalDiagnostics() }
    fun checkCapabilitiesDiagnostics() { refreshCapabilities(); runLocalDiagnostics() }
    fun clearDiagnostics() { diagnosticHistory.clear() }
    private fun recordVoiceMetric(metric: VoiceMetric, durationMs: Long) {
        latencyTracker.record(metric, durationMs)
        diagnosticHistory.record(DiagnosticCheck("voice.${metric.name.lowercase()}", "voice", DiagnosticStatus.PASS,
            EvidenceLevel.PENDING, System.currentTimeMillis(), durationMs,
            description = "Duración técnica ${metric.name}; no acredita calidad acústica."))
    }
    private fun recordVoiceFailure(stage: String, code: String) {
        diagnosticHistory.record(DiagnosticCheck("voice.${stage.lowercase()}", "voice",
            if (code == "CANCELLED") DiagnosticStatus.CANCELLED else DiagnosticStatus.FAIL,
            EvidenceLevel.PENDING, System.currentTimeMillis(), errorCode = code,
            description = "Etapa de voz $stage: $code. La respuesta escrita permanece disponible.",
            correctiveAction = if (code == "CANCELLED") null else "Revisa permisos, servicio y voz instalada; reintenta manualmente."))
    }
    fun listen() { if (mutable.value.preferences.onboarded) voice.listen() }
    fun demo(text: String) { if (mutable.value.preferences.onboarded) voice.demo(text) }
    fun cancel() { voice.cancel() }
    fun savePreferences(value: VoicePreferences) {
        if (mutable.value.preferences.onboarded) cancel()
        viewModelScope.launch {
            try { preferences.save(value) } catch (_: java.io.IOException) { mutable.update { it.copy(notice = "No se pudieron guardar las preferencias.") } }
        }
    }
    fun saveCredential(value: String) {
        try {
            secrets.put(value)
            mutable.update { it.copy(credentialSaved = true, notice = "Credencial guardada en este dispositivo.") }
        } catch (_: Exception) { mutable.update { it.copy(notice = "No se pudo guardar la credencial.") } }
    }
    fun clearCredential() {
        cancel(); secrets.clear()
        mutable.update { it.copy(credentialSaved = false, notice = "Credencial eliminada.") }
    }
    fun clearConversation() { assistant.clearAllConversations(); mutable.update { it.copy(notice = "Conversación borrada.") } }
    fun setDemoPlaces(enabled: Boolean) {
        demoPlaces = enabled; places.clear(); refreshCapabilities()
        mutable.update { it.copy(places = places.state.value.copy(status = if (enabled) "DEMO_READY" else if (it.placesConfigured) "READY" else "NOT_CONFIGURED"),
            notice = if (enabled) "Lugares de demostración; sin consulta real." else "Demostración desactivada.") }
    }
    fun setPlacesConsent(allowed: Boolean) {
        placesConsent = allowed
        if (!allowed) places.clear()
        mutable.update { it.copy(placesConsent = allowed) }
    }
    fun setPlacesStationary(confirmed: Boolean) {
        stationaryPlaces = confirmed
        stationaryConfirmedAt = if (confirmed) clock.now() else 0
        mutable.update { it.copy(placesStationary = confirmed) }
    }
    fun searchPlaces(category: PlaceCategory) { viewModelScope.launch {
        val started = SystemClock.elapsedRealtime()
        val result = tools.execute(ToolRequest("places.search_nearby", mapOf("query" to category.label)))
        diagnosticHistory.record(DiagnosticCheck("places.search", "places",
            if (result.executionStatus == ExecutionStatus.SUCCESS) DiagnosticStatus.PASS else DiagnosticStatus.FAIL,
            if (demoPlaces) EvidenceLevel.FAKE else EvidenceLevel.PENDING, clock.now(),
            (SystemClock.elapsedRealtime() - started).coerceAtLeast(0), result.errorCode,
            if (demoPlaces) "Consulta de simulación; respuesta ${places.state.value.status}." else "Consulta Google Places; respuesta ${places.state.value.status}."))
        mutable.update { it.copy(notice = result.message) }
    } }
    fun selectPlace(id: String) { viewModelScope.launch {
        val result = tools.execute(ToolRequest("places.select_result", mapOf("id" to id)))
        mutable.update { it.copy(notice = result.message) }
    } }
    fun openSelectedPlace(provider: String) { viewModelScope.launch {
        val result = tools.execute(ToolRequest("navigation.open_selected_place", mapOf("provider" to provider)))
        mutable.update { it.copy(notice = result.message) }
    } }
    fun clearPlaces() { places.clear(); mutable.update { it.copy(notice = "Resultados temporales borrados.") } }
    fun testProvider() { viewModelScope.launch {
        val started = SystemClock.elapsedRealtime()
        val result = aiProvider.generate(AIRequest("Responde solo: conexión disponible."))
        val code = when (result) {
            is AIResponse.Unavailable -> result.error.code
            is AIResponse.Invalid -> result.error.code
            else -> null
        }
        diagnosticHistory.record(DiagnosticCheck("ai.interoperability", "ai",
            if (code == null) DiagnosticStatus.PASS else DiagnosticStatus.FAIL, EvidenceLevel.PENDING,
            System.currentTimeMillis(), (SystemClock.elapsedRealtime() - started).coerceAtLeast(0), code,
            if (code == null) "Respuesta estructural recibida; compatibilidad completa sin verificar." else "Consulta de prueba falló: $code.",
            if (code == null) null else "Revisa endpoint, cuota, credencial y conexión."))
        mutable.update { it.copy(notice = when (result) {
            is AIResponse.Text, is AIResponse.ProposedTool -> "Proveedor respondió."
            is AIResponse.Unavailable -> result.error.message
            is AIResponse.Invalid -> result.error.message
        }) }
    } }
    fun testVoice() { viewModelScope.launch {
        val started = SystemClock.elapsedRealtime()
        try {
            tts.speak("Prueba de voz en español.")
            diagnosticHistory.record(DiagnosticCheck("tts.callback", "voice", DiagnosticStatus.PASS, EvidenceLevel.PENDING,
                System.currentTimeMillis(), (SystemClock.elapsedRealtime() - started).coerceAtLeast(0),
                description = "Callback TTS completado; calidad audible requiere confirmación humana."))
            mutable.update { it.copy(notice = "Motor TTS finalizó. Confirma tú si se escuchó bien.") }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) {
            diagnosticHistory.record(DiagnosticCheck("tts.callback", "voice", DiagnosticStatus.FAIL, EvidenceLevel.PENDING,
                System.currentTimeMillis(), (SystemClock.elapsedRealtime() - started).coerceAtLeast(0), "TTS_ERROR",
                "La reproducción TTS falló; respuesta escrita disponible.", "Instala o selecciona una voz local es-ES."))
            mutable.update { it.copy(notice = "No se pudo reproducir la voz; la respuesta escrita sigue disponible.") }
        }
    } }
    fun installVoiceData() {
        try {
            app.startActivity(android.content.Intent(android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: android.content.ActivityNotFoundException) {
            mutable.update { it.copy(notice = "No hay instalador de voces en este dispositivo.") }
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
    override fun onCleared() { background(); bluetooth.dispose(); assistant.clearAllConversations(); tts.close(); super.onCleared() }
}
