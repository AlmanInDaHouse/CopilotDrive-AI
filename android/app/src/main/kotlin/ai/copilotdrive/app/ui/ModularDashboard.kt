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
import androidx.compose.ui.semantics.*
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
    onAcknowledgeMode: () -> Unit = {},
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { EssentialControls(state, onSettings, onListen, onCancel, onDemo) }) { insets ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(insets)) {
            val fontScale = LocalDensity.current.fontScale
            val columns = dashboardColumns(maxWidth.value, fontScale)
            val shortLandscape = maxHeight / fontScale < 420.dp && maxWidth / fontScale >= 600.dp

            val projected = remember(layout, columns) { LayoutEngine.project(layout, columns) }
            val rows = remember(projected) { projected.groupBy { it.placement.row }.values.toList() }
            LazyColumn(Modifier.fillMaxSize().testTag("dashboard"), contentPadding = PaddingValues(horizontal = if (maxWidth >= 800.dp) 24.dp else 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item(key = "header") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (shortLandscape) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Eyebrow("COPILOTDRIVE AI"); Text(state.clock, color = Muted) }
                                ModeBadge(state.preferences.operationMode)
                                TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp).testTag("edit-dashboard")) { Text("Personalizar") }
                                OutlinedButton(onClick = onPlaces, shape = Cockpit.ControlShape, modifier = Modifier.heightIn(min = 48.dp).testTag("open-places")) { Text("Lugares") }
                            }
                        } else {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("COPILOTDRIVE AI", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                Text("TU CABINA, BAJO CONTROL", color = Muted, style = MaterialTheme.typography.labelSmall)
                            }
                            Text(state.clock, style = MaterialTheme.typography.titleLarge, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ModeBadge(state.preferences.operationMode)
                            Text(if (state.preferences.operationMode == OperationMode.DEMO) "Sin conexión al coche" else "Servicios locales", color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onPlaces, shape = Cockpit.ControlShape,
                                modifier = Modifier.heightIn(min = 48.dp).testTag("open-places")) {
                                CockpitMark(CockpitSymbol.ROUTE, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Lugares")
                            }
                            TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp).testTag("edit-dashboard")) { Text("Personalizar") }
                        }
                        }
                        layoutNotice?.let { Text(it, color = Amber) }
                        if (state.preferences.modeNoticeRequired) {
                            DashboardCard(Modifier.fillMaxWidth()) {
                                Text("Ahora empiezas en REAL", style = MaterialTheme.typography.titleLarge)
                                Text("La simulación anterior era predeterminada. Conservamos tus ajustes y widgets. Sin OBD no verás datos del vehículo; puedes elegir DEMO en Ajustes.", color = Muted)
                                TextButton(onClick = onAcknowledgeMode, modifier = Modifier.testTag("acknowledge-real-mode")) { Text("Entendido") }
                            }
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
    Surface(color = Cockpit.Background, tonalElevation = 0.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HorizontalDivider(color = Cockpit.Border)
            Text(if (state.preferences.operationMode == OperationMode.DEMO) "DEMO · Datos de vehículo ficticios"
                else if (state.vehicleConnection == VehicleConnectionState.CONNECTED_REAL) "REAL · OBD conectado · ${state.vehicle?.source ?: "Sin datos"}"
                else "REAL · ${connectionLabel(state.vehicleConnection)}", color = if (state.preferences.operationMode == OperationMode.DEMO) Amber else Muted,
                fontSize = 12.sp, modifier = Modifier.testTag("telemetry-source"))
            TextButton(onClick = { fullResponse = true }, contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("voice-status").semantics {
                    liveRegion = LiveRegionMode.Polite
                    stateDescription = if (microphone) "Micrófono activo" else "Micrófono apagado"
                }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (microphone) "MICRÓFONO ACTIVO · ${state.voice.message}"
                        else if (state.voice.message.startsWith("Micrófono apagado")) state.voice.message else "Micrófono apagado · ${state.voice.message}",
                        color = if (microphone) Amber else Muted, maxLines = 2, fontSize = 13.sp,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start, modifier = Modifier.weight(1f))
                    Text("Leer respuesta", color = Mint, fontSize = 13.sp, maxLines = 2,
                        modifier = Modifier.widthIn(max = 128.dp))
                }
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
    Button(onClick = if (busy) onCancel else onListen, shape = Cockpit.ControlShape, modifier = modifier.heightIn(min = 56.dp).testTag("push-to-talk")) {
        CockpitMark(CockpitSymbol.VOICE, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onPrimary)
        Spacer(Modifier.width(10.dp))
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
            Eyebrow("HORA LOCAL"); Text(state.clock, color = Mint, style = Cockpit.Numbers)
            if (!compact) Text("Hora del dispositivo", color = Muted)
        }
        WidgetType.SYSTEM_STATUS -> SystemStatusWidget(state.capabilities, state.diagnosticSummary, onDeviceLab, compact, preview)
    }
}

@Composable private fun NavigationWidget(state: DashboardState, onNavigate: (String) -> Unit, preview: Boolean) {
    val selected = state.places.results.firstOrNull { it.providerId == state.places.selection?.selectedId }
    val name = selected?.name ?: state.destination
    DashboardCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { CockpitMark(CockpitSymbol.ROUTE); Eyebrow("PRÓXIMO DESTINO") }
        Text(name ?: "¿Adónde vamos?", fontSize = 22.sp, modifier = Modifier.padding(vertical = 12.dp))
        Text(if (state.places.demo && selected != null) "SIMULACIÓN · sin ruta activa" else "Maps o Waze calcularán la ruta. Sin ETA disponible.", color = Muted, fontSize = 13.sp)
        listOf("maps" to "Maps", "waze" to "Waze").forEach { (provider, label) ->
            val available = state.capabilities.any { it.id == "navigation.${if (provider == "maps") "google_maps" else "waze"}" && it.status == CapabilityStatus.AVAILABLE }
            OutlinedButton(onClick = { onNavigate(provider) }, enabled = !preview && name != null && (available || state.places.demo),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Abrir $label") }
            if (!available) Text("$label no disponible${if (state.places.demo) " · demo" else ""}", color = Amber, fontSize = 12.sp)
        }
    }
}

@Composable private fun AssistantWidget(voice: VoiceSessionState, onListen: () -> Unit, compact: Boolean, preview: Boolean) {
    DashboardCard(Modifier.fillMaxWidth(), role = PanelRole.VOICE) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AssistantOrb(voice.state)
            Column { Text("JARVIS", fontSize = 24.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Medium); Eyebrow(voiceLabel(voice.state)) }
        }
        Text(voice.message, fontSize = 20.sp, modifier = Modifier.testTag("assistant-message").padding(vertical = 12.dp))
        voice.errorSource?.let { Text("Revisar $it", color = Amber) }
        Text("Una pulsación. Tu voz. Tú decides.", color = Muted, fontSize = 13.sp)
        OutlinedButton(onClick = onListen, enabled = !preview && voice.state !in setOf(VoiceState.LISTENING, VoiceState.TRANSCRIBING,
            VoiceState.UNDERSTANDING, VoiceState.EXECUTING, VoiceState.SPEAKING),
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
        Text(connectionLabel(connection), fontSize = 20.sp, color = if (connection == VehicleConnectionState.CONNECTED_REAL) Mint else Amber,
            modifier = Modifier.padding(vertical = 12.dp))
        Text(if (connection == VehicleConnectionState.CONNECTED_REAL) "ELM conectado · solo lectura" else "Selecciona un adaptador en Ajustes", color = Muted)
        Text("${pids.size} PIDs disponibles", color = Muted)
        if (!compact) Text("PIDs: ${pids.sorted().joinToString().ifBlank { "No disponibles" }}", color = Muted)
        error?.let { Text(it, color = Amber) }
        OutlinedButton(onClick = onDeviceLab, enabled = !preview, modifier = Modifier.heightIn(min = 56.dp)) { Text("Diagnóstico OBD") }
    }
}

@Composable private fun SystemStatusWidget(capabilities: List<Capability>, diagnostics: List<DiagnosticCheck>, onDeviceLab: () -> Unit, compact: Boolean, preview: Boolean) {
    DashboardCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { CockpitMark(CockpitSymbol.SYSTEM); Eyebrow("SISTEMA") }
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

fun connectionLabel(state: VehicleConnectionState): String = when (state) {
    VehicleConnectionState.SIMULATED -> "DEMO · vehículo simulado"
    VehicleConnectionState.CONNECTING -> "Conectando e inicializando…"
    VehicleConnectionState.CONNECTED_REAL -> "OBD conectado"
    VehicleConnectionState.DISCONNECTED -> "OBD desconectado"
    VehicleConnectionState.UNSUPPORTED -> "Bluetooth no compatible"
    VehicleConnectionState.STALE -> "Reconectando · datos obsoletos"
    VehicleConnectionState.ERROR -> "Error de conexión · reintenta manualmente"
}
