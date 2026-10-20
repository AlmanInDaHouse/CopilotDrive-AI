package ai.copilotdrive.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.copilotdrive.app.ui.*
import ai.copilotdrive.core.DiagnosticStatus
import ai.copilotdrive.core.VoiceMetric

@Composable
fun DeviceLabScreen(model: DashboardViewModel, onBack: () -> Unit, onPlaces: () -> Unit = {}) {
    val checks by model.diagnostics.collectAsStateWithLifecycle()
    val latencies by model.voiceLatencies.collectAsStateWithLifecycle()
    var providerWarning by remember { mutableStateOf(false) }
    var voiceWarning by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.testTag("device-lab")) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Laboratorio del dispositivo", style = MaterialTheme.typography.headlineMedium)
                OutlinedButton(onClick = onBack) { Text("Volver") }
            }
            Text("Disponibilidad no equivale a funcionamiento verificado. Las pruebas físicas requieren intervención y vehículo detenido.", color = Muted)
        }
        item {
            DashboardCard {
                Eyebrow("PRUEBAS LOCALES")
                OutlinedButton(onClick = model::runLocalDiagnostics, modifier = Modifier.testTag("run-local-diagnostics")) { Text("Ejecutar diagnóstico local") }
                OutlinedButton(onClick = model::checkVoiceDiagnostics) { Text("Comprobar voz instalada") }
                OutlinedButton(onClick = model::checkBluetoothDiagnostics) { Text("Comprobar Bluetooth y emparejamiento") }
                OutlinedButton(onClick = model::checkCapabilitiesDiagnostics) { Text("Consultar capacidades") }
                Text("Estas comprobaciones no abren micrófono, conexión OBD ni red.", color = Muted)
            }
        }
        item {
            DashboardCard {
                Eyebrow("PRUEBAS ACTIVAS")
                OutlinedButton(onClick = { voiceWarning = true }) { Text("Prueba acústica TTS") }
                OutlinedButton(onClick = { providerWarning = true }) { Text("Comprobar proveedor IA") }
                OutlinedButton(onClick = onPlaces) { Text("Probar lugares desde Lugares") }
                Text("La conexión OBD se inicia desde Ajustes con confirmación. Usa solo un vehículo detenido.", color = Muted)
            }
        }
        item {
            DashboardCard {
                Eyebrow("TIEMPOS DE VOZ")
                Text("p50/p95 se muestran desde 20 observaciones por etapa; son tiempos técnicos, no calidad audible.", color = Muted)
                VoiceMetric.entries.forEach { metric ->
                    val summary = latencies[metric]
                    Text("${metric.name}: n=${summary?.count ?: 0}" +
                        if (summary?.p50Ms != null) " · p50=${summary.p50Ms} ms · p95=${summary.p95Ms} ms" else " · muestra insuficiente")
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Resultados (${checks.size})", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = model::clearDiagnostics) { Text("Borrar historial") }
            }
            if (checks.isEmpty()) Text("Sin comprobaciones en esta sesión.", color = Muted)
        }
        items(checks.asReversed()) { check ->
            DashboardCard {
                Text("${check.component} · ${check.id}", style = MaterialTheme.typography.titleMedium)
                Text("${check.status.name} · evidencia ${check.evidence.name}",
                    color = if (check.status == DiagnosticStatus.PASS) Mint else Amber)
                Text(check.description)
                Text("${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(check.timestampMs))} · ${check.durationMs?.let { "$it ms" } ?: "sin medición"}", color = Muted)
                check.errorCode?.let { Text("Código: $it", color = Amber) }
                check.correctiveAction?.let { Text(it, color = Muted) }
            }
        }
    }
    if (voiceWarning) AlertDialog(onDismissRequest = { voiceWarning = false },
        title = { Text("Prueba acústica") },
        text = { Text("Android reproducirá una frase por el altavoz. El callback indica finalización técnica; confirma tú si se escuchó correctamente.") },
        confirmButton = { TextButton(onClick = { voiceWarning = false; model.testVoice() }) { Text("Reproducir") } },
        dismissButton = { TextButton(onClick = { voiceWarning = false }) { Text("Cancelar") } })
    if (providerWarning) AlertDialog(onDismissRequest = { providerWarning = false },
        title = { Text("Consulta externa opcional") },
        text = { Text("Esta prueba envía una frase fija al proveedor configurado, requiere consentimiento de IA y puede consumir cuota. No incluye telemetría.") },
        confirmButton = { TextButton(onClick = { providerWarning = false; model.testProvider() }) { Text("Enviar prueba") } },
        dismissButton = { TextButton(onClick = { providerWarning = false }) { Text("Cancelar") } })
}
