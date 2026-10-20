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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.copilotdrive.app.DashboardState
import ai.copilotdrive.core.*

val Mint = Color(0xFFB7F6CE)
val Surface = Color(0xFF141C21)
val Muted = Color(0xFFA7B4BC)
val Amber = Color(0xFFF1BC79)

@Composable
fun CopilotTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Mint, onPrimary = Color(0xFF10251B), background = Color(0xFF0B1014),
        surface = Surface, onSurface = Color(0xFFF0F5F3), onSurfaceVariant = Muted,
        secondary = Amber, outline = Color(0xFF324047),
    ), content = content)
}

@Composable
fun DashboardScreen(
    state: DashboardState,
    onSettings: () -> Unit,
    onListen: () -> Unit,
    onCancel: () -> Unit,
    onDemo: (String) -> Unit,
    onMedia: (MediaControl) -> Unit,
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
fun DashboardCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(22.dp), color = Surface, border = BorderStroke(1.dp, Color(0xFF29363D))) {
        Column(Modifier.padding(22.dp), content = content)
    }
}

@Composable fun Eyebrow(text: String) { Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, color = Mint) }

fun metricLabel(metric: Metric?): String {
    val value = metric?.value
    return if (value != null && metric.availability in setOf(Availability.AVAILABLE, Availability.SIMULATED)) "${formatNumber(value)} ${metric.unit}" else "No disponible"
}

@Composable
private fun VehicleCard(vehicle: VehicleSnapshot?, modifier: Modifier = Modifier) {
    DashboardCard(modifier.fillMaxWidth()) {
        Eyebrow(if (vehicle?.simulationMode == true) "01 / VEHÍCULO SIMULADO" else "01 / VEHÍCULO OBD")
        Spacer(Modifier.height(18.dp))
        Text("Combustible", color = Muted)
        Text(metricLabel(vehicle?.fuelLevelPercent), fontSize = 44.sp, fontWeight = FontWeight.Light, color = Mint, modifier = Modifier.testTag("fuel-value"))
        val fuel = vehicle?.fuelLevelPercent
        val fraction = if (fuel?.availability in setOf(Availability.AVAILABLE, Availability.SIMULATED)) ((fuel?.value ?: 0.0) / 100).toFloat().coerceIn(0f, 1f) else 0f
        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp), color = Mint, trackColor = Color(0xFF2B3B34))
        MetricRow("Velocidad", metricLabel(vehicle?.speedKmh))
        MetricRow("Refrigerante", metricLabel(vehicle?.coolantTemperatureC))
        MetricRow("Autonomía", metricLabel(vehicle?.vehicleRangeKm))
        Text(if (vehicle?.simulationMode == true) "Datos ficticios" else if (vehicle?.connectionStatus == ConnectionStatus.CONNECTED) "Lecturas OBD · ${vehicle.source}" else "OBD sin conectar",
            color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable private fun MetricRow(name: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, color = Muted, fontSize = 14.sp)
        Text(value, fontSize = 14.sp)
    }
}

@Composable
private fun MediaCard(media: MediaSnapshot, artwork: Bitmap?, onMedia: (MediaControl) -> Unit, modifier: Modifier = Modifier) {
    DashboardCard(modifier.fillMaxWidth()) {
        Eyebrow(if (media.simulated) "02 / AUDIO SIMULADO" else "02 / AUDIO ANDROID")
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (artwork != null) {
                Image(artwork.asImageBitmap(), contentDescription = "Carátula", contentScale = ContentScale.Crop, modifier = Modifier.size(68.dp).clip(RoundedCornerShape(12.dp)))
            } else Canvas(Modifier.size(68.dp).background(Color(0xFF263B39), RoundedCornerShape(12.dp))) {
                drawCircle(Mint.copy(alpha = 0.25f), size.minDimension * 0.36f, style = Stroke(2.dp.toPx()))
                drawCircle(Mint.copy(alpha = 0.6f), size.minDimension * 0.24f, style = Stroke(2.dp.toPx()))
                drawCircle(Mint, size.minDimension * 0.06f)
            }
            Column(Modifier.weight(1f)) {
                Text(media.title ?: "Tu música, aquí", fontSize = 20.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(media.artist ?: "Abre un reproductor compatible", color = Muted, fontSize = 13.sp, maxLines = 2)
            }
        }
        Text(media.sourceApp ?: "Sin sesión", color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 14.dp))
        Text(if (media.simulated) "Demostración de controles · sin sonido" else media.playbackStatus.name.lowercase(), fontSize = 12.sp, color = Amber, modifier = Modifier.padding(top = 6.dp))
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

@Composable private fun AssistantOrb(state: VoiceState) {
    val active = state in setOf(VoiceState.LISTENING, VoiceState.SPEAKING)
    val radius by animateFloatAsState(if (active) 0.40f else 0.28f, label = "voice-state")
    Canvas(Modifier.size(68.dp)) {
        drawCircle(Mint.copy(alpha = 0.14f), size.minDimension * 0.49f)
        drawCircle(if (state == VoiceState.ERROR) Amber else Mint, size.minDimension * radius, style = Stroke(2.dp.toPx()))
        drawCircle(Mint, size.minDimension * 0.08f)
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
