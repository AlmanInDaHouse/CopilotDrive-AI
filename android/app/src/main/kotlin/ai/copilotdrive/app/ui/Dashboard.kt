package ai.copilotdrive.app.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.copilotdrive.app.DashboardState
import ai.copilotdrive.core.*

val Mint = Cockpit.Mint
val Surface = Cockpit.Panel
val Muted = Cockpit.Muted
val Amber = Cockpit.Warning

@Composable
fun CopilotTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Mint, onPrimary = Color(0xFF10251B), background = Cockpit.Background,
        surface = Surface, onSurface = Cockpit.Ink, onBackground = Cockpit.Ink,
        surfaceVariant = Cockpit.Raised, onSurfaceVariant = Muted,
        secondary = Mint, error = Cockpit.Error, outline = Cockpit.Border,
    ), typography = Cockpit.Typography,
        shapes = Shapes(small = Cockpit.ControlShape, medium = Cockpit.PanelShape, large = Cockpit.PanelShape), content = content)
}

@Composable
fun LegacyDashboardScreen(
    state: DashboardState,
    onSettings: () -> Unit,
    onListen: () -> Unit,
    onCancel: () -> Unit,
    onDemo: (String) -> Unit,
    onMedia: (MediaControl) -> Unit,
    onPlaces: () -> Unit = {},
) {
    val busy = state.voice.state in setOf(VoiceState.LISTENING, VoiceState.TRANSCRIBING, VoiceState.UNDERSTANDING, VoiceState.EXECUTING, VoiceState.SPEAKING)
    val microphoneActive = state.voice.state in setOf(VoiceState.LISTENING, VoiceState.TRANSCRIBING)
    Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text(if (microphoneActive) "MICRÓFONO ACTIVO" else state.voice.message, color = if (microphoneActive) Amber else Muted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("voice-status"))
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = if (busy) onCancel else onListen, modifier = Modifier.weight(1f).heightIn(min = 60.dp).testTag("push-to-talk")) {
                        Text(if (busy) "Detener" else "Pulsar para hablar", fontSize = 17.sp)
                    }
                    OutlinedButton(onClick = { onDemo("Jarvis, cuánta gasolina queda") }, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 60.dp).testTag("demo-fuel")) { Text("Probar consulta") }
                }
            }
        }
    }) { insets ->
    LazyColumn(modifier = Modifier.fillMaxSize().padding(insets).testTag("dashboard"), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("COPILOT / DRIVE", fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 20.sp)
                    Text("Asistente local · Sin servidor", color = Muted, fontSize = 13.sp)
                }
                Text(state.clock, fontSize = 24.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(horizontal = 16.dp))
                OutlinedButton(onClick = onSettings, modifier = Modifier.heightIn(min = 56.dp), enabled = !busy) { Text("Ajustes") }
                OutlinedButton(onClick = onPlaces, modifier = Modifier.heightIn(min = 56.dp).testTag("open-places"), enabled = !busy) { Text("Lugares") }
            }
        }
        item {
            Surface(color = Color(0xFF29251E), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF695235))) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(Amber, CircleShape))
                    Text(if (state.vehicle?.simulationMode == true) "SIMULADOR · Datos de vehículo ficticios"
                        else "OBD · ${state.vehicleConnection.name} · ${state.vehicle?.source ?: "Sin datos"}", color = Amber, fontWeight = FontWeight.Medium)
                }
            }
        }
        item {
            BoxWithConstraints {
                if (maxWidth >= 760.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        VehicleCard(state.vehicle, Modifier.weight(1.15f))
                        MediaCard(state.media, state.artwork, onMedia, Modifier.weight(1f))
                        NavigationCard(state.destination, onListen, Modifier.weight(0.9f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        VehicleCard(state.vehicle)
                        MediaCard(state.media, state.artwork, onMedia)
                        NavigationCard(state.destination, onListen)
                    }
                }
            }
        }
        item {
            DashboardCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    AssistantOrb(state.voice.state)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Eyebrow("JARVIS / ${voiceLabel(state.voice.state)}${state.voice.errorSource?.let { " / $it" } ?: ""}")
                        Text(state.voice.message, fontSize = 20.sp, lineHeight = 28.sp, modifier = Modifier.testTag("assistant-message"))
                        Text(if (microphoneActive) "MICRÓFONO ACTIVO" else "Micrófono apagado · Solo escucha al pulsar", color = if (microphoneActive) Amber else Muted, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Di «cuánta gasolina queda», «pausa la música» o «llévame a Gran Vía, Madrid».", color = Muted, fontSize = 13.sp)
            }
        }
        item {
            Text(state.notice, color = Muted, fontSize = 13.sp)
            Text("ENTORNO DE PRUEBA DETENIDO · Sin control del vehículo", modifier = Modifier.padding(top = 8.dp), color = Amber, fontSize = 11.sp, letterSpacing = 1.sp)
        }
    }
    }
}

@Composable
fun DashboardCard(modifier: Modifier = Modifier, role: PanelRole = PanelRole.STANDARD, content: @Composable ColumnScope.() -> Unit) {
    val color = when (role) { PanelRole.STANDARD -> Surface; PanelRole.VEHICLE -> Cockpit.Raised; PanelRole.VOICE -> Cockpit.VoiceSurface }
    Surface(modifier, shape = if (role == PanelRole.VOICE) Cockpit.VoiceShape else Cockpit.PanelShape,
        color = color, contentColor = Cockpit.Ink, border = if (role == PanelRole.STANDARD) BorderStroke(1.dp, Cockpit.Border) else null) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable fun Eyebrow(text: String) { Text(text, modifier = Modifier.semantics { heading() }, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, color = Mint) }

fun metricLabel(metric: Metric?): String {
    val value = metric?.value
    return if (value != null && metric.availability in setOf(Availability.AVAILABLE, Availability.SIMULATED)) "${formatNumber(value)} ${metric.unit}" else "No disponible"
}

@Composable
internal fun VehicleCard(vehicle: VehicleSnapshot?, modifier: Modifier = Modifier, compact: Boolean = false) {
    val demo = vehicle?.simulationMode == true
    val connected = vehicle?.connectionStatus == ConnectionStatus.CONNECTED
    DashboardCard(modifier.fillMaxWidth(), role = PanelRole.VEHICLE) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CockpitMark(CockpitSymbol.VEHICLE)
            Eyebrow(if (demo) "VEHÍCULO / DEMO" else "VEHÍCULO / OBD-II")
        }
        Text(if (demo) "Cabina de demostración" else if (connected) "Vehículo conectado" else "Conecta tu vehículo",
            style = if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 4.dp))
        val sourceLabel = if (demo) "Datos ficticios · sin lectura del coche" else if (connected) "${vehicle?.source} · solo lectura" else "OBD desconectado · conexión manual en Ajustes"
        if (!compact) {
            Text(sourceLabel, color = if (demo) Amber else Muted, fontSize = 13.sp)
            HorizontalDivider(color = Cockpit.Border, modifier = Modifier.padding(vertical = 8.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text("COMBUSTIBLE", color = Muted, style = MaterialTheme.typography.labelSmall)
                val fuelText = telemetryLabel(vehicle, vehicle?.fuelLevelPercent, "012F")
                Text(fuelText, color = if (connected) Mint else Cockpit.Ink,
                    style = if (vehicle?.fuelLevelPercent?.value != null && vehicle.fuelLevelPercent.availability in setOf(Availability.AVAILABLE, Availability.SIMULATED)) Cockpit.Numbers else MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.testTag("fuel-value").padding(top = 4.dp))
            }
        }
        val fuel = vehicle?.fuelLevelPercent
        if (fuel?.availability in setOf(Availability.AVAILABLE, Availability.SIMULATED) && connected) {
            LinearProgressIndicator(progress = { ((fuel?.value ?: 0.0) / 100).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clearAndSetSemantics {}, color = Mint, trackColor = Cockpit.Raised)
        }
        if (compact) Text(sourceLabel, color = if (demo) Amber else Muted, fontSize = 13.sp)
        MetricRow("Velocidad", telemetryLabel(vehicle, vehicle?.speedKmh, "010D"))
        MetricRow("Motor", telemetryLabel(vehicle, vehicle?.engineRpm, "010C"))
        if (!compact) {
            MetricRow("Refrigerante", telemetryLabel(vehicle, vehicle?.coolantTemperatureC, "0105"))
            MetricRow("Autonomía", telemetryLabel(vehicle, vehicle?.vehicleRangeKm))
        }
        if (connected && !demo) Text("Fuente: ${vehicle?.source} · las lecturas caducan a los 10 s", color = Muted, fontSize = 12.sp)
    }
}

@Composable private fun MetricRow(name: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp).semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(name, color = Muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, modifier = Modifier.weight(1.15f), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}

@Composable
internal fun MediaCard(media: MediaSnapshot, artwork: Bitmap?, onMedia: (MediaControl) -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    DashboardCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { CockpitMark(CockpitSymbol.AUDIO); Eyebrow(if (media.simulated) "AUDIO / DEMO" else "MULTIMEDIA") }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (artwork != null && !compact) {
                Image(artwork.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(68.dp).clip(RoundedCornerShape(12.dp)))
            } else if (!compact) Canvas(Modifier.size(68.dp).background(Color(0xFF263B39), RoundedCornerShape(12.dp))) {
                drawCircle(Mint.copy(alpha = 0.25f), size.minDimension * 0.36f, style = Stroke(2.dp.toPx()))
                drawCircle(Mint.copy(alpha = 0.6f), size.minDimension * 0.24f, style = Stroke(2.dp.toPx()))
                drawCircle(Mint, size.minDimension * 0.06f)
            }
            Column(Modifier.weight(1f)) {
                Text(media.title ?: "Sin reproducción", fontSize = 20.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(media.artist ?: "Selecciona una sesión real en Ajustes", color = Muted, fontSize = 13.sp, maxLines = 2)
            }
        }
        Text(media.sourceApp ?: "Sin sesión", color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 14.dp))
        Text(if (media.simulated) "Demostración de controles · sin sonido" else when (media.playbackStatus) { PlaybackStatus.PLAYING -> "Reproduciendo"; PlaybackStatus.PAUSED -> "En pausa"; PlaybackStatus.STOPPED -> "Detenido"; PlaybackStatus.UNAVAILABLE -> "Esperando una sesión Android" }, fontSize = 12.sp, color = Amber, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(16.dp))
        val duration = media.durationMs
        val position = media.positionMs
        if (duration != null && position != null) {
            LinearProgressIndicator(progress = { (position.toFloat() / duration.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Mint)
        }
        Spacer(Modifier.height(12.dp))
        val control = if (media.playbackStatus == PlaybackStatus.PLAYING) MediaControl.PAUSE else MediaControl.PLAY
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { onMedia(MediaControl.PREVIOUS) }, enabled = MediaControl.PREVIOUS in media.supportedControls, contentPadding = PaddingValues(0.dp), modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text("Anterior", fontSize = 12.sp) }
            Button(onClick = { onMedia(control) }, enabled = control in media.supportedControls, contentPadding = PaddingValues(0.dp), modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text(if (control == MediaControl.PLAY) "Play" else "Pausa") }
            OutlinedButton(onClick = { onMedia(MediaControl.NEXT) }, enabled = MediaControl.NEXT in media.supportedControls, contentPadding = PaddingValues(0.dp), modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text("Siguiente", fontSize = 12.sp) }
        }
        media.reason?.let { Text(it, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
        if (media.supportedControls.isEmpty()) Text("El reproductor no ofrece controles disponibles.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun NavigationCard(destination: String?, onListen: () -> Unit, modifier: Modifier = Modifier) {
    DashboardCard(modifier.fillMaxWidth()) {
        Eyebrow("03 / PRÓXIMO DESTINO")
        Spacer(Modifier.height(18.dp))
        Canvas(Modifier.fillMaxWidth().height(64.dp)) {
            val line = Color(0xFF2D4148)
            for (i in 1..4) drawLine(line, Offset(size.width * i / 5, 0f), Offset(size.width * i / 5 - 25f, size.height), 1.dp.toPx())
            drawLine(line, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.dp.toPx())
            drawLine(Mint, Offset(size.width * 0.18f, size.height * 0.8f), Offset(size.width * 0.5f, size.height * 0.3f), 3.dp.toPx())
            drawLine(Mint, Offset(size.width * 0.5f, size.height * 0.3f), Offset(size.width * 0.82f, size.height * 0.3f), 3.dp.toPx())
            drawCircle(Mint, 5.dp.toPx(), Offset(size.width * 0.82f, size.height * 0.3f))
        }
        Text(destination ?: "¿Adónde vamos?", fontSize = 22.sp, maxLines = 2, modifier = Modifier.padding(top = 12.dp))
        Text("Maps / Waze · app externa", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        Text("Sin datos de ETA ni distancia", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onListen, modifier = Modifier.heightIn(min = 56.dp).fillMaxWidth()) { Text("Decir destino") }
    }
}

@Composable internal fun AssistantOrb(state: VoiceState) {
    val active = state in setOf(VoiceState.LISTENING, VoiceState.SPEAKING)
    val tint = if (state == VoiceState.ERROR) Cockpit.Error else Mint
    Canvas(Modifier.size(64.dp).clearAndSetSemantics {}) {
        drawCircle(tint.copy(alpha = .08f), size.minDimension * .49f)
        drawArc(tint.copy(alpha = .4f), 140f, 260f, false, style = Stroke(1.dp.toPx()))
        val heights = if (active) listOf(.25f, .55f, .8f, .45f, .65f) else listOf(.12f, .3f, .48f, .3f, .12f)
        heights.forEachIndexed { i, height ->
            val x = size.width * (.28f + i * .11f)
            drawLine(tint, Offset(x, size.height * (.5f - height * .28f)), Offset(x, size.height * (.5f + height * .28f)), 2.dp.toPx())
        }
    }
}

fun voiceLabel(state: VoiceState): String = when (state) {
    VoiceState.IDLE -> "LISTO"
    VoiceState.LISTENING -> "ESCUCHANDO"
    VoiceState.TRANSCRIBING, VoiceState.UNDERSTANDING, VoiceState.EXECUTING -> "PROCESANDO"
    VoiceState.SPEAKING -> "RESPONDIENDO"
    VoiceState.PERMISSION_REQUIRED -> "PERMISO NECESARIO"
    VoiceState.CANCELLED -> "CANCELADO"
    VoiceState.ERROR -> "REVISAR VOZ"
}
