package ai.copilotdrive.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.copilotdrive.app.DashboardState
import ai.copilotdrive.core.*

/** All external actions are callbacks to the existing app/tool boundary. Rendering performs no IO. */
@Composable
fun DashboardScreen(
    state: DashboardState, onSettings: () -> Unit, onListen: () -> Unit, onCancel: () -> Unit,
    onDemo: (String) -> Unit, onMedia: (MediaControl) -> Unit, onPlaces: () -> Unit = {},
    layout: DashboardLayout = WidgetLayouts.preset(DashboardPreset.CLASSIC),
    onEdit: () -> Unit = {}, onDeviceLab: () -> Unit = {}, onNavigate: (String) -> Unit = {},
    layoutNotice: String? = null,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { EssentialControls(state, onSettings, onListen, onCancel, onDemo) }) { insets ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(insets)) {
            val fontScale = LocalDensity.current.fontScale
            val columns = dashboardColumns(maxWidth.value, fontScale)
            val shortLandscape = maxHeight / fontScale < 420.dp && maxWidth / fontScale >= 600.dp
            val projected = remember(layout, columns) { LayoutEngine.project(layout, columns) }
            val rows = remember(projected) { projected.groupBy { it.placement.row }.values.toList() }
            LazyColumn(Modifier.fillMaxSize().testTag("dashboard"), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item(key = "header") {
                    if (shortLandscape) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Eyebrow("COPILOT / DRIVE"); Text(state.clock, color = Mint, fontSize = 22.sp) }
                            OutlinedButton(onClick = onEdit, modifier = Modifier.heightIn(min = 56.dp).testTag("edit-dashboard")) { Text("Personalizar") }
                            OutlinedButton(onClick = onPlaces, modifier = Modifier.heightIn(min = 56.dp).testTag("open-places")) { Text("Lugares") }
                        }
                        layoutNotice?.let { Text(it, color = Amber) }
                    } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Eyebrow("COPILOT / DRIVE")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tu viaje, a tu manera", fontSize = 24.sp, modifier = Modifier.weight(1f))
                            Text(state.clock, color = Mint, fontSize = 24.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onEdit, modifier = Modifier.heightIn(min = 56.dp).testTag("edit-dashboard")) { Text("Personalizar") }
                            OutlinedButton(onClick = onPlaces, modifier = Modifier.heightIn(min = 56.dp).testTag("open-places")) { Text("Lugares") }
                        }
                        layoutNotice?.let { Text(it, color = Amber) }
                    }
                    }
                }
                items(rows, key = { row -> row.joinToString { it.id } }) { row ->
                    WidgetRow(row, columns, state, onMedia, onPlaces, onListen, onDeviceLab, onNavigate)
                }
                item(key = "notice") { Text(state.notice, color = Muted, fontSize = 13.sp) }
            }
        }
    }
}

fun dashboardColumns(widthDp: Float, fontScale: Float): Int = when {
    widthDp / fontScale.coerceAtLeast(1f) >= 1120 -> 4
    widthDp / fontScale.coerceAtLeast(1f) >= 600 -> 2
    else -> 1
}

@Composable
internal fun EssentialControls(state: DashboardState, onSettings: () -> Unit, onListen: () -> Unit,
    onCancel: () -> Unit, onDemo: (String) -> Unit) {
    val busy = state.voice.state in setOf(VoiceState.LISTENING, VoiceState.TRANSCRIBING, VoiceState.UNDERSTANDING,
        VoiceState.EXECUTING, VoiceState.SPEAKING)
    val microphone = state.voice.state in setOf(VoiceState.LISTENING, VoiceState.TRANSCRIBING)
    var fullResponse by remember { mutableStateOf(false) }
    if (fullResponse) AlertDialog(onDismissRequest = { fullResponse = false },
        title = { Text("Jarvis · ${voiceLabel(state.voice.state)}") },
        text = { Text(state.voice.message, modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { fullResponse = false }) { Text("Cerrar") } })
    Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 6.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (state.vehicle?.simulationMode == true) "SIMULADOR · Datos de vehículo ficticios"
                else "OBD · ${state.vehicleConnection.name} · ${state.vehicle?.source ?: "Sin datos"}",
                color = Amber, fontSize = 12.sp, modifier = Modifier.testTag("telemetry-source"))
            TextButton(onClick = { fullResponse = true }, contentPadding = PaddingValues(0.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("voice-status")) {
                Text(if (microphone) "MICRÓFONO ACTIVO · ${state.voice.message}" else "${state.voice.message} · Leer respuesta",
                    color = if (microphone) Amber else Muted, maxLines = 2, fontSize = 13.sp,
                    modifier = Modifier.weight(1f))
            }
            BoxWithConstraints {
                if (maxWidth / LocalDensity.current.fontScale >= 440.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoiceButton(busy, onListen, onCancel, Modifier.weight(1f))
                        OutlinedButton(onClick = { onDemo("Jarvis, cuánta gasolina queda") }, enabled = !busy,
                            modifier = Modifier.heightIn(min = 56.dp).testTag("demo-fuel")) { Text("Probar consulta") }
                        OutlinedButton(onClick = onSettings, modifier = Modifier.heightIn(min = 56.dp)) { Text("Ajustes") }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    VoiceButton(busy, onListen, onCancel, Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onDemo("Jarvis, cuánta gasolina queda") }, enabled = !busy,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("demo-fuel")) { Text("Probar consulta") }
                        OutlinedButton(onClick = onSettings, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Ajustes") }
                    }
                    }
                }
            }
        }
    }
}

@Composable private fun VoiceButton(busy: Boolean, onListen: () -> Unit, onCancel: () -> Unit, modifier: Modifier) {
    Button(onClick = if (busy) onCancel else onListen, modifier = modifier.heightIn(min = 56.dp).testTag("push-to-talk")) {
        Text(if (busy) "Detener" else "Pulsar para hablar")
    }
}

@Composable
internal fun WidgetRow(row: List<WidgetInstance>, columns: Int, state: DashboardState,
    onMedia: (MediaControl) -> Unit, onPlaces: () -> Unit, onListen: () -> Unit, onDeviceLab: () -> Unit,
    onNavigate: (String) -> Unit, preview: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        var used = 0
        row.sortedBy { it.placement.column }.forEach { widget ->
            if (widget.placement.column > used) Spacer(Modifier.weight((widget.placement.column - used).toFloat()))
            key(widget.id) {
                Box(Modifier.weight(widget.size.columns.toFloat()).testTag("widget-${widget.id}")) {
                    WidgetContent(widget, state, onMedia, onPlaces, onListen, onDeviceLab, onNavigate, preview)
                }
            }
            used = widget.placement.column + widget.size.columns
        }
        if (used < columns) Spacer(Modifier.weight((columns - used).toFloat()))
    }
}

@Composable
private fun WidgetContent(widget: WidgetInstance, state: DashboardState, onMedia: (MediaControl) -> Unit,
    onPlaces: () -> Unit, onListen: () -> Unit, onDeviceLab: () -> Unit, onNavigate: (String) -> Unit, preview: Boolean) {
    val compact = widget.size == WidgetSize.COMPACT
    when (widget.type) {
        WidgetType.VEHICLE -> VehicleCard(state.vehicle, compact = compact)
        WidgetType.MEDIA -> MediaCard(state.media, state.artwork, if (preview) ({ _ -> }) else onMedia, compact = compact)
        WidgetType.NAVIGATION -> NavigationWidget(state, onNavigate, preview)
        WidgetType.ASSISTANT -> AssistantWidget(state.voice, onListen, compact, preview)
        WidgetType.PLACES -> PlacesWidget(state.places, onPlaces, compact, preview)
        WidgetType.OBD_STATUS -> ObdStatusWidget(state.vehicleConnection, state.bluetoothStatus, state.obdCapabilities, state.obdError, onDeviceLab, compact, preview)
        WidgetType.CLOCK -> DashboardCard(Modifier.fillMaxWidth()) {
            Eyebrow("HORA LOCAL"); Text(state.clock, color = Mint, fontSize = if (compact) 32.sp else 48.sp)
            if (!compact) Text("Hora del dispositivo", color = Muted)
        }
        WidgetType.SYSTEM_STATUS -> SystemStatusWidget(state.capabilities, state.diagnosticSummary, onDeviceLab, compact, preview)
    }
}

@Composable private fun NavigationWidget(state: DashboardState, onNavigate: (String) -> Unit, preview: Boolean) {
    val selected = state.places.results.firstOrNull { it.providerId == state.places.selection?.selectedId }
    val name = selected?.name ?: state.destination
    DashboardCard(Modifier.fillMaxWidth()) {
        Eyebrow("NAVEGACIÓN")
        Text(name ?: "Sin destino seleccionado", fontSize = 22.sp, modifier = Modifier.padding(vertical = 12.dp))
        Text(if (state.places.demo && selected != null) "SIMULACIÓN · sin ruta activa" else "Aplicación externa · sin ETA disponible", color = Muted)
        listOf("maps" to "Maps", "waze" to "Waze").forEach { (provider, label) ->
            val available = state.capabilities.any { it.id == "navigation.${if (provider == "maps") "google_maps" else "waze"}" && it.status == CapabilityStatus.AVAILABLE }
            OutlinedButton(onClick = { onNavigate(provider) }, enabled = !preview && name != null && (available || state.places.demo),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Abrir $label") }
            if (!available) Text("$label no disponible${if (state.places.demo) " · demo" else ""}", color = Amber, fontSize = 12.sp)
        }
    }
}

@Composable private fun AssistantWidget(voice: VoiceSessionState, onListen: () -> Unit, compact: Boolean, preview: Boolean) {
    DashboardCard(Modifier.fillMaxWidth()) {
        Eyebrow("JARVIS / ${voiceLabel(voice.state)}")
        if (!compact) AssistantOrb(voice.state)
        Text(voice.message, fontSize = 20.sp, modifier = Modifier.testTag("assistant-message").padding(vertical = 12.dp))
        voice.errorSource?.let { Text("Revisar $it", color = Amber) }
        Text("PTT y micrófono siempre en la barra inferior", color = Muted, fontSize = 12.sp)
        OutlinedButton(onClick = onListen, enabled = !preview && voice.state !in setOf(VoiceState.LISTENING, VoiceState.SPEAKING),
            modifier = Modifier.heightIn(min = 56.dp)) { Text("Hablar con Jarvis") }
    }
}

@Composable private fun PlacesWidget(places: PlacesState, onPlaces: () -> Unit, compact: Boolean, preview: Boolean) {
    DashboardCard(Modifier.fillMaxWidth()) {
        Eyebrow(if (places.demo) "LUGARES SIMULADOS" else "LUGARES / ${places.status}")
        val selected = places.results.firstOrNull { it.providerId == places.selection?.selectedId }
        Text(selected?.name ?: "${places.results.size} resultados en sesión", fontSize = 20.sp, modifier = Modifier.padding(vertical = 12.dp))
        selected?.rating?.let { Text("Valoración: $it / 5", color = Mint) }
        if (!compact) places.results.take(3).forEach { place ->
            Text("${if (place == selected) "✓ " else ""}${place.name ?: "Sin nombre"}${place.rating?.let { " · $it / 5" } ?: ""}", color = Muted)
        }
        places.error?.let { Text(it, color = Amber) }
        Text("Búsqueda solo al solicitarla", color = Muted, fontSize = 12.sp)
        OutlinedButton(onClick = onPlaces, enabled = !preview, modifier = Modifier.heightIn(min = 56.dp)) { Text("Abrir Lugares") }
    }
}

@Composable private fun ObdStatusWidget(connection: VehicleConnectionState, bluetooth: String, pids: Set<String>, error: String?,
    onDeviceLab: () -> Unit, compact: Boolean, preview: Boolean) {
    DashboardCard(Modifier.fillMaxWidth()) {
        Eyebrow("BLUETOOTH / ELM327")
        Text("Bluetooth: $bluetooth", color = Muted)
        Text(connection.name, fontSize = 20.sp, color = if (connection == VehicleConnectionState.CONNECTED_REAL) Mint else Amber,
            modifier = Modifier.padding(vertical = 12.dp))
        Text(if (connection == VehicleConnectionState.CONNECTED_REAL) "ELM conectado · solo lectura" else "Conexión real no verificada", color = Muted)
        Text("${pids.size} PIDs disponibles", color = Muted)
        if (!compact) Text("PIDs: ${pids.sorted().joinToString().ifBlank { "No disponibles" }}", color = Muted)
        error?.let { Text(it, color = Amber) }
        OutlinedButton(onClick = onDeviceLab, enabled = !preview, modifier = Modifier.heightIn(min = 56.dp)) { Text("Diagnóstico OBD") }
    }
}

@Composable private fun SystemStatusWidget(capabilities: List<Capability>, diagnostics: List<DiagnosticCheck>, onDeviceLab: () -> Unit, compact: Boolean, preview: Boolean) {
    DashboardCard(Modifier.fillMaxWidth()) {
        Eyebrow("SISTEMA")
        Text("${capabilities.count { it.status == CapabilityStatus.AVAILABLE }} / ${capabilities.size} capacidades disponibles",
            fontSize = 20.sp, modifier = Modifier.padding(vertical = 12.dp))
        if (!compact) capabilities.filter { it.status != CapabilityStatus.AVAILABLE }.take(4).forEach {
            Text("${it.reason}: ${it.status.name}", color = Muted)
        }
        if (!compact) diagnostics.forEach { Text("${it.status}: ${it.description}", color = Muted, fontSize = 12.sp) }
        Text("Disponibilidad no acredita validación física", color = Amber, fontSize = 12.sp)
        OutlinedButton(onClick = onDeviceLab, enabled = !preview, modifier = Modifier.heightIn(min = 56.dp).testTag("widget-device-lab")) { Text("Device Lab") }
    }
}
