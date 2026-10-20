package ai.copilotdrive.app

import androidx.activity.compose.BackHandler
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.copilotdrive.app.platform.VoicePreferences
import ai.copilotdrive.app.ui.*
import ai.copilotdrive.core.CapabilityStatus
import ai.copilotdrive.core.PlaceCategory
import ai.copilotdrive.core.dashboardReady

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun CopilotApp(model: DashboardViewModel, onMicrophone: () -> Unit, onContacts: () -> Unit, onMediaPermission: () -> Unit,
    onBluetoothPermission: () -> Unit = {}, onPlaceSearch: (PlaceCategory) -> Unit = {}) {
    val state by model.state.collectAsStateWithLifecycle()
    val customization by model.customization.state.collectAsStateWithLifecycle()
    var settings by rememberSaveable { mutableStateOf(false) }
    var deviceLab by rememberSaveable { mutableStateOf(false) }
    var places by rememberSaveable { mutableStateOf(false) }
    var confirmSettings by remember { mutableStateOf(false) }
    var confirmEditor by remember { mutableStateOf(false) }
    var laboratoryMode by remember { mutableStateOf(false) }
    val dashboardIsReady = dashboardReady(state.preferencesLoaded, state.preferences.onboarded,
        customization.loaded, state.vehicle != null) && !settings && !deviceLab && !places &&
        customization.edit == null && !confirmSettings && !confirmEditor
    ReportDrawnWhen { dashboardIsReady }
    LaunchedEffect(customization.laboratory) { if (!customization.laboratory) laboratoryMode = false }
    CopilotTheme {
        Surface(Modifier.fillMaxSize().safeDrawingPadding().semantics { testTagsAsResourceId = true }
            .testTag(if (dashboardIsReady) "dashboard-ready" else "startup-loading"), color = MaterialTheme.colorScheme.background) {
            when {
                !state.preferencesLoaded -> Box(Modifier.padding(32.dp)) { Text("Preparando CopilotDrive…") }
                !state.preferences.onboarded -> Onboarding { model.savePreferences(state.preferences.copy(onboarded = true)) }
                deviceLab -> DeviceLabScreen(model, { model.cancel(); deviceLab = false }, { model.cancel(); deviceLab = false; places = true }, onMicrophone)
                places -> PlacesScreen(state, model, { places = false }, onPlaceSearch)
                settings -> SettingsScreen(state, { settings = false }, model::savePreferences, { settings = false; onMicrophone() }, onContacts, onMediaPermission, onBluetoothPermission, model::setDemoMedia, model.androidMedia::select, model, { deviceLab = true })
                customization.edit != null -> DashboardEditor(state, customization, model.customization::change, model::saveLayout,
                    model.customization::cancel, { model.customization.confirmLaboratory(false) },
                    { model.customization.cancel(); confirmSettings = true }, onMicrophone, model::cancel, model::demo)
                else -> DashboardScreen(state, { confirmSettings = true }, onMicrophone, model::cancel, model::demo, model::mediaControl,
                    onPlaces = { places = true; model.cancel() }, layout = customization.layout,
                    onEdit = { laboratoryMode = false; confirmEditor = true; model.cancel() },
                    onDeviceLab = { deviceLab = true; model.cancel() }, onNavigate = model::openDashboardNavigation,
                    layoutNotice = customization.error)
            }
            if (confirmSettings) {
                AlertDialog(onDismissRequest = { confirmSettings = false }, title = { Text("Configuración de laboratorio") },
                    text = { Text("El movimiento del vehículo es desconocido. Abre los ajustes únicamente en un entorno de prueba detenido.") },
                    confirmButton = { TextButton(onClick = { confirmSettings = false; settings = true; model.cancel() }) { Text("Estoy en un entorno detenido") } },
                    dismissButton = { TextButton(onClick = { confirmSettings = false }) { Text("Cancelar") } })
            }
            if (confirmEditor) {
                AlertDialog(onDismissRequest = { confirmEditor = false }, title = { Text("Edición solo en laboratorio") },
                    text = { Column {
                        Text("Movimiento: ${customization.motion.name}. Desconocido no significa detenido. Confirma solo en un entorno de prueba detenido; no verifica aparcamiento.")
                        Row {
                            Checkbox(checked = laboratoryMode, onCheckedChange = { laboratoryMode = it }, modifier = Modifier.testTag("layout-laboratory").semantics { contentDescription = "Activar modo laboratorio" })
                            Text("Activar modo laboratorio", modifier = Modifier.padding(top = 12.dp))
                        }
                    } },
                    confirmButton = { TextButton(onClick = {
                        model.customization.confirmLaboratory(laboratoryMode)
                        if (model.customization.begin()) confirmEditor = false
                    }, enabled = laboratoryMode && customization.motion != ai.copilotdrive.core.MotionState.MOVING && customization.loaded,
                        modifier = Modifier.testTag("confirm-layout-laboratory")) { Text("Confirmo entorno de prueba detenido") } },
                    dismissButton = { TextButton(onClick = { confirmEditor = false; laboratoryMode = false }) { Text("Cancelar") } })
            }
        }
    }
    BackHandler(settings || deviceLab || places || customization.edit != null) {
        if (customization.edit != null) model.customization.cancel()
        else if (places) places = false else if (deviceLab) { model.cancel(); deviceLab = false } else settings = false
    }
}

@Composable
fun Onboarding(onAccept: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(32.dp), verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.testTag("onboarding")) {
        item { Eyebrow("COPILOTDRIVE AI / VISTA PREVIA") }
        item { Text("Un copiloto.\nDatos claros.\nTu control.", fontSize = 40.sp, lineHeight = 46.sp) }
        item { Text("Empieza con un vehículo simulado y consultas locales. No necesitas cuenta, servidor ni claves.", color = Muted, fontSize = 18.sp) }
        item {
            DashboardCard {
                Text("Antes de empezar", fontSize = 22.sp)
                Spacer(Modifier.height(14.dp))
                Text("• Usa esta versión solo en un entorno de prueba detenido.\n• La telemetría es ficticia y no describe tu coche.\n• El micrófono se activa al pulsar y pedir permiso.\n• El motor de voz del sistema puede usar servicios externos; ese fallback está desactivado hasta que lo autorices en Ajustes.\n• No guardamos conversaciones ni audio.", color = Muted, lineHeight = 25.sp)
            }
        }
        item { Button(onClick = onAccept, modifier = Modifier.heightIn(min = 64.dp).testTag("accept-onboarding")) { Text("Empezar en entorno de prueba") } }
    }
}

@Composable
private fun SettingsScreen(
    state: DashboardState, onBack: () -> Unit, onSave: (VoicePreferences) -> Unit,
    onMicrophone: () -> Unit, onContacts: () -> Unit, onMediaPermission: () -> Unit,
    onBluetoothPermission: () -> Unit, onDemoMedia: (Boolean) -> Unit, onSelectSession: (String) -> Unit, model: DashboardViewModel,
    onDeviceLab: () -> Unit,
) {
    var voiceMenu by remember { mutableStateOf(false) }
    var rate by remember(state.preferences.rate) { mutableFloatStateOf(state.preferences.rate) }
    var pitch by remember(state.preferences.pitch) { mutableFloatStateOf(state.preferences.pitch) }
    var endpoint by remember(state.preferences.ai.endpoint) { mutableStateOf(state.preferences.ai.endpoint) }
    var providerModel by remember(state.preferences.ai.model) { mutableStateOf(state.preferences.ai.model) }
    var providerName by remember(state.preferences.ai.providerName) { mutableStateOf(state.preferences.ai.providerName) }
    var timeout by remember(state.preferences.ai.timeoutMs) { mutableFloatStateOf(state.preferences.ai.timeoutMs.toFloat()) }
    var token by remember { mutableStateOf("") }
    var consentDialog by remember { mutableStateOf(false) }
    var obdConsentDialog by remember { mutableStateOf(false) }
    var providerTestDialog by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.testTag("settings")) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tu copiloto", fontSize = 30.sp)
                OutlinedButton(onClick = onBack, modifier = Modifier.heightIn(min = 56.dp)) { Text("Volver") }
            }
            Text("Configuración de laboratorio · Movimiento desconocido", color = Amber)
        }
        item {
            DashboardCard(Modifier.fillMaxWidth()) {
                Eyebrow("LABORATORIO DEL DISPOSITIVO")
                Text("Comprueba disponibilidad y revisa observaciones sin iniciar pruebas físicas automáticamente.", color = Muted)
                OutlinedButton(onClick = onDeviceLab, modifier = Modifier.testTag("open-device-lab")) { Text("Abrir laboratorio") }
            }
        }
        item {
            DashboardCard(Modifier.fillMaxWidth()) {
                Eyebrow("VOZ Y PRIVACIDAD")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Permitir motor STT del sistema\nPuede enviar audio a su proveedor", color = Muted, modifier = Modifier.weight(1f).padding(vertical = 16.dp))
                    Switch(modifier = Modifier.semantics { contentDescription = "Permitir motor STT del sistema; puede enviar audio a su proveedor" }, checked = state.preferences.allowSystemRecognition, onCheckedChange = { onSave(state.preferences.copy(allowSystemRecognition = it)) })
                }
                Text("Se prefiere reconocimiento local cuando está disponible. No hay escucha permanente.", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Box {
                    OutlinedButton(onClick = { voiceMenu = true }, modifier = Modifier.heightIn(min = 56.dp)) { Text(if (state.preferences.voice.isBlank()) "Voz local en español (automática)" else state.preferences.voice) }
                    DropdownMenu(expanded = voiceMenu, onDismissRequest = { voiceMenu = false }, modifier = Modifier.heightIn(max = 300.dp)) {
                        DropdownMenuItem(text = { Text("Español automático") }, onClick = { onSave(state.preferences.copy(voice = "")); voiceMenu = false })
                        state.voices.forEach { voice -> DropdownMenuItem(text = { Text(voice.label) }, onClick = { onSave(state.preferences.copy(voice = voice.name)); voiceMenu = false }) }
                    }
                }
                Text(state.ttsStatus, color = if (state.voices.isEmpty()) Amber else Mint)
                if (state.voices.isEmpty()) OutlinedButton(onClick = model::installVoiceData) { Text("Instalar datos de voz desde Android") }
                Text("Velocidad de voz: ${"%.1f".format(rate)}")
                Slider(modifier = Modifier.semantics { contentDescription = "Velocidad de voz" }, value = rate, onValueChange = { rate = it }, onValueChangeFinished = { onSave(state.preferences.copy(rate = rate)) }, valueRange = 0.7f..1.3f)
                Text("Tono: ${"%.1f".format(pitch)}")
                Slider(modifier = Modifier.semantics { contentDescription = "Tono de voz" }, value = pitch, onValueChange = { pitch = it }, onValueChangeFinished = { onSave(state.preferences.copy(pitch = pitch)) }, valueRange = 0.7f..1.3f)
                OutlinedButton(onClick = onMicrophone, modifier = Modifier.heightIn(min = 56.dp)) { Text("Permitir micrófono y probar") }
                OutlinedButton(onClick = { model.testVoice() }, enabled = state.voices.isNotEmpty()) { Text("Probar voz seleccionada") }
            }
        }
        item {
            DashboardCard(Modifier.fillMaxWidth()) {
                Eyebrow("MULTIMEDIA Y CONTACTOS")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Usar audio de demostración", modifier = Modifier.weight(1f).padding(vertical = 16.dp))
                    Switch(modifier = Modifier.semantics { contentDescription = "Usar audio de demostración" }, checked = state.demoMedia, onCheckedChange = onDemoMedia)
                }
                Text("Para audio real, desactiva la demostración y concede acceso a notificaciones. Solo consultamos sesiones multimedia; no leemos ni guardamos el contenido de notificaciones.", color = Muted)
                OutlinedButton(onClick = onMediaPermission, modifier = Modifier.heightIn(min = 56.dp).padding(top = 8.dp)) { Text("Autorizar sesiones multimedia") }
                state.sessions.forEach { session ->
                    TextButton(onClick = { onSelectSession(session.id) }, modifier = Modifier.heightIn(min = 56.dp)) { Text("Elegir ${session.label}${if (session.playing) " · reproduciendo" else ""}") }
                }
                OutlinedButton(onClick = onContacts, modifier = Modifier.heightIn(min = 56.dp).padding(top = 8.dp)) { Text("Activar búsqueda de contactos") }
                Text("Contactos solo en el dispositivo. «Llama a Mercedes» pide confirmación y abre el marcador; nunca llama automáticamente.", color = Muted, fontSize = 13.sp)
            }
        }
        item {
            DashboardCard(Modifier.fillMaxWidth()) {
                Eyebrow("VEHÍCULO · OBD-II")
                Text("Estado: ${state.vehicleConnection.name}. Lecturas locales; sin diagnóstico certificado.", color = Muted)
                Text("Experimental. Usa la app únicamente con el vehículo detenido. Empareja primero un adaptador ELM327 Bluetooth Classic en Ajustes de Android.", color = Muted, fontSize = 13.sp)
                OutlinedButton(onClick = onBluetoothPermission) { Text("Mostrar dispositivos emparejados") }
                state.obdDevices.forEach { device ->
                    TextButton(onClick = { model.selectObd(device.address) }) {
                        Text("${if (state.selectedObdAddress == device.address) "✓ " else ""}${device.name} · ${device.address}")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { obdConsentDialog = true }, enabled = state.selectedObdAddress != null &&
                        state.vehicleConnection != ai.copilotdrive.core.VehicleConnectionState.CONNECTED_REAL) { Text("Conectar") }
                    OutlinedButton(onClick = model::disconnectObd) { Text("Desconectar") }
                }
                OutlinedButton(onClick = model::useSimulator) { Text("Volver al simulador") }
                Text("PIDs disponibles: ${state.obdCapabilities.sorted().joinToString().ifBlank { "No disponible" }}", color = Muted)
                val lastReading = state.vehicle?.takeIf { !it.simulationMode && it.connectionStatus == ai.copilotdrive.core.ConnectionStatus.CONNECTED &&
                    listOf(it.speedKmh, it.engineRpm, it.coolantTemperatureC, it.fuelLevelPercent).any { metric -> metric.availability == ai.copilotdrive.core.Availability.AVAILABLE } }?.capturedAt
                Text("Última lectura: ${lastReading?.let { java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "No disponible"}", color = Muted)
                state.obdError?.let { Text(it, color = Amber) }
            }
        }
        item {
            DashboardCard(Modifier.fillMaxWidth()) {
                Eyebrow("INTELIGENCIA ARTIFICIAL")
                Text("Comandos locales · siempre activos", modifier = Modifier.padding(top = 12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Activar IA externa", modifier = Modifier.weight(1f).padding(vertical = 16.dp))
                    Switch(modifier = Modifier.semantics { contentDescription = "Activar IA externa" }, checked = state.preferences.ai.enabled, onCheckedChange = { enabled ->
                        if (enabled && !state.preferences.ai.consented) consentDialog = true
                        else onSave(state.preferences.copy(ai = state.preferences.ai.copy(enabled = enabled)))
                    })
                }
                OutlinedTextField(value = endpoint, onValueChange = { endpoint = it.take(300) }, label = { Text("URL base del proveedor") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = providerName, onValueChange = { providerName = it.take(80) }, label = { Text("Nombre del proveedor") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = providerModel, onValueChange = { providerModel = it.take(100) }, label = { Text("Modelo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Tiempo de espera: ${timeout.toInt() / 1000} s")
                Slider(modifier = Modifier.semantics { contentDescription = "Tiempo de espera del proveedor" }, value = timeout, onValueChange = { timeout = it }, valueRange = 3_000f..30_000f, steps = 8)
                Text("HTTPS obligatorio. HTTP local solo en APK debug para localhost o 10.0.2.2.", color = Muted, fontSize = 13.sp)
                OutlinedButton(onClick = {
                    val changed = endpoint != state.preferences.ai.endpoint || providerModel != state.preferences.ai.model || providerName != state.preferences.ai.providerName
                    onSave(state.preferences.copy(ai = state.preferences.ai.copy(endpoint = endpoint.trim(), model = providerModel.trim(),
                        providerName = providerName.trim(), timeoutMs = timeout.toInt(),
                        enabled = if (changed) false else state.preferences.ai.enabled,
                        consented = if (changed) false else state.preferences.ai.consented)))
                }) { Text("Guardar proveedor y modelo") }
                OutlinedTextField(value = token, onValueChange = { token = it.take(4_096) }, label = { Text("Credencial") },
                    visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { model.saveCredential(token); token = "" }, enabled = token.isNotBlank()) { Text("Guardar clave") }
                    OutlinedButton(onClick = model::clearCredential, enabled = state.credentialSaved) { Text("Eliminar clave") }
                }
                Text(if (state.credentialSaved) "Credencial guardada y oculta" else "No hay credencial", color = Muted)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Herramientas estructuradas", modifier = Modifier.weight(1f))
                    Switch(modifier = Modifier.semantics { contentDescription = "Herramientas estructuradas" }, checked = state.preferences.ai.toolCalling, onCheckedChange = { onSave(state.preferences.copy(ai = state.preferences.ai.copy(toolCalling = it))) })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Compartir contexto del vehículo", modifier = Modifier.weight(1f))
                    Switch(modifier = Modifier.semantics { contentDescription = "Compartir contexto del vehículo"; stateDescription = "No disponible; solo se envía texto" }, checked = false, onCheckedChange = {}, enabled = false)
                }
                Text("En este sprint solo se envía el texto y el historial efímero. No se envían contactos, ubicación ni telemetría.", color = Muted, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { providerTestDialog = true }, enabled = state.preferences.ai.enabled && state.preferences.ai.consented) { Text("Probar conexión") }
                    OutlinedButton(onClick = { onSave(state.preferences.copy(ai = state.preferences.ai.copy(enabled = false, consented = false))) }) { Text("Revocar permiso") }
                }
                OutlinedButton(onClick = model::clearConversation) { Text("Borrar conversación") }
                Text(state.notice, color = Muted)
            }
        }
        item { Eyebrow("DIAGNÓSTICO DEL DISPOSITIVO") }
        items(state.capabilities, key = { it.id }) { capability ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(capability.reason, modifier = Modifier.weight(1f))
                Text(capabilityLabel(capability.status), color = if (capability.status == CapabilityStatus.AVAILABLE) Mint else Amber)
            }
        }
    }
    if (consentDialog) AlertDialog(onDismissRequest = { consentDialog = false },
        title = { Text("Autorizar consulta externa") },
        text = { Text("Proveedor: ${state.preferences.ai.providerName.ifBlank { state.preferences.ai.endpoint.ifBlank { "sin configurar" } }}. Se enviará el texto de la consulta y hasta ocho mensajes recientes. No se enviarán contactos, ubicación ni telemetría. Puedes desactivar y borrar la clave en cualquier momento.") },
        confirmButton = { TextButton(onClick = {
            consentDialog = false
            onSave(state.preferences.copy(ai = state.preferences.ai.copy(enabled = true, consented = true)))
        }) { Text("Autorizar") } },
        dismissButton = { TextButton(onClick = { consentDialog = false }) { Text("Cancelar") } })
    if (obdConsentDialog) AlertDialog(onDismissRequest = { obdConsentDialog = false },
        title = { Text("Conectar vehículo experimental") },
        text = { Text("La app leerá RPM, velocidad, refrigerante y combustible cuando el vehículo lo admita. No es un diagnóstico certificado. La telemetría permanece local y no se envía a proveedores de IA.") },
        confirmButton = { TextButton(onClick = { obdConsentDialog = false; model.connectObd() }) { Text("Conectar") } },
        dismissButton = { TextButton(onClick = { obdConsentDialog = false }) { Text("Cancelar") } })
    if (providerTestDialog) AlertDialog(onDismissRequest = { providerTestDialog = false },
        title = { Text("Consulta de prueba externa") },
        text = { Text("Se enviará una frase fija al proveedor configurado. Puede consumir cuota. No se enviará telemetría, contactos ni ubicación.") },
        confirmButton = { TextButton(onClick = { providerTestDialog = false; model.testProvider() }) { Text("Enviar prueba") } },
        dismissButton = { TextButton(onClick = { providerTestDialog = false }) { Text("Cancelar") } })
}

fun capabilityLabel(status: CapabilityStatus): String = when (status) {
    CapabilityStatus.AVAILABLE -> "Disponible"
    CapabilityStatus.PERMISSION_REQUIRED -> "Pendiente de permiso"
    CapabilityStatus.UNSUPPORTED -> "No compatible"
    CapabilityStatus.NOT_CONFIGURED -> "No configurado"
    CapabilityStatus.DISCONNECTED -> "Desconectado"
    CapabilityStatus.ERROR -> "Error"
}
