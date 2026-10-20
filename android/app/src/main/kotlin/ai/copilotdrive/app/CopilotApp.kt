package ai.copilotdrive.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.copilotdrive.app.platform.VoicePreferences
import ai.copilotdrive.app.ui.*
import ai.copilotdrive.core.CapabilityStatus

@Composable
fun CopilotApp(model: DashboardViewModel, onMicrophone: () -> Unit, onContacts: () -> Unit, onMediaPermission: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    var settings by rememberSaveable { mutableStateOf(false) }
    var confirmSettings by remember { mutableStateOf(false) }
    CopilotTheme {
        Surface(Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
            when {
                !state.preferencesLoaded -> Box(Modifier.padding(32.dp)) { Text("Preparando CopilotDrive…") }
                !state.preferences.onboarded -> Onboarding { model.savePreferences(state.preferences.copy(onboarded = true)) }
                settings -> SettingsScreen(state, { settings = false }, model::savePreferences, { settings = false; onMicrophone() }, onContacts, onMediaPermission, model::setDemoMedia, model.androidMedia::select)
                else -> DashboardScreen(state, { confirmSettings = true }, onMicrophone, model::cancel, model::demo, model::mediaControl)
            }
            if (confirmSettings) {
                AlertDialog(onDismissRequest = { confirmSettings = false }, title = { Text("Configuración de laboratorio") },
                    text = { Text("El movimiento del vehículo es desconocido. Abre los ajustes únicamente en un entorno de prueba detenido.") },
                    confirmButton = { TextButton(onClick = { confirmSettings = false; settings = true; model.cancel() }) { Text("Estoy en un entorno detenido") } },
                    dismissButton = { TextButton(onClick = { confirmSettings = false }) { Text("Cancelar") } })
            }
        }
    }
    BackHandler(settings) { settings = false }
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
    onDemoMedia: (Boolean) -> Unit, onSelectSession: (String) -> Unit,
) {
    var voiceMenu by remember { mutableStateOf(false) }
    var rate by remember(state.preferences.rate) { mutableFloatStateOf(state.preferences.rate) }
    var pitch by remember(state.preferences.pitch) { mutableFloatStateOf(state.preferences.pitch) }
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
                Eyebrow("VOZ Y PRIVACIDAD")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Permitir motor STT del sistema\nPuede enviar audio a su proveedor", color = Muted, modifier = Modifier.weight(1f).padding(vertical = 16.dp))
                    Switch(checked = state.preferences.allowSystemRecognition, onCheckedChange = { onSave(state.preferences.copy(allowSystemRecognition = it)) })
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
                if (state.voices.isEmpty()) Text("No se detectan voces locales. Instala una voz TTS en los ajustes de Android.", color = Amber)
                Text("Velocidad de voz: ${"%.1f".format(rate)}")
                Slider(value = rate, onValueChange = { rate = it }, onValueChangeFinished = { onSave(state.preferences.copy(rate = rate)) }, valueRange = 0.7f..1.3f)
                Text("Tono: ${"%.1f".format(pitch)}")
                Slider(value = pitch, onValueChange = { pitch = it }, onValueChangeFinished = { onSave(state.preferences.copy(pitch = pitch)) }, valueRange = 0.7f..1.3f)
                OutlinedButton(onClick = onMicrophone, modifier = Modifier.heightIn(min = 56.dp)) { Text("Permitir micrófono y probar") }
            }
        }
        item {
            DashboardCard(Modifier.fillMaxWidth()) {
                Eyebrow("MULTIMEDIA Y CONTACTOS")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Usar audio de demostración", modifier = Modifier.weight(1f).padding(vertical = 16.dp))
                    Switch(checked = state.demoMedia, onCheckedChange = onDemoMedia)
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
                Eyebrow("PROVEEDORES")
                Text("Asistente local · activo", modifier = Modifier.padding(top = 12.dp))
                Text("IA remota, Bluetooth OBD, wake word y lugares cercanos: no configurados en esta versión. El APK no solicita acceso a Internet.", color = Muted)
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
}

fun capabilityLabel(status: CapabilityStatus): String = when (status) {
    CapabilityStatus.AVAILABLE -> "Disponible"
    CapabilityStatus.PERMISSION_REQUIRED -> "Pendiente de permiso"
    CapabilityStatus.UNSUPPORTED -> "No compatible"
    CapabilityStatus.NOT_CONFIGURED -> "No configurado"
    CapabilityStatus.DISCONNECTED -> "Desconectado"
    CapabilityStatus.ERROR -> "Error"
}
