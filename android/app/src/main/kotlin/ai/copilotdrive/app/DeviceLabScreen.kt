package ai.copilotdrive.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.copilotdrive.app.ui.*
import ai.copilotdrive.core.DiagnosticStatus
import ai.copilotdrive.core.*

@Composable
fun DeviceLabScreen(model: DashboardViewModel, onBack: () -> Unit, onPlaces: () -> Unit = {}, onMicrophone: () -> Unit = {}) {
    val acoustic by model.acousticState.collectAsStateWithLifecycle()
    val journey by model.voiceJourney.collectAsStateWithLifecycle()
    val checks by model.diagnostics.collectAsStateWithLifecycle()
    val latencies by model.voiceLatencies.collectAsStateWithLifecycle()
    var providerWarning by remember { mutableStateOf(false) }
    var voiceWarning by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.testTag("device-lab")) {
        item {
            ScreenHeading("DIAGNÓSTICOS", "Device Lab", onBack)
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
            AcousticProbePanel(acoustic, { voiceWarning = true }, model::cancel, model::confirmAcoustic)
        }
        item {
            DashboardCard {
                Text("Recorrido Jarvis", modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                Text("Di: cuánta gasolina queda. La respuesta del simulador se conserva en el dashboard. El diagnóstico muestra solo etapas, sin transcripción ni argumentos.", color = Muted)
                OutlinedButton(onClick = onMicrophone, modifier = Modifier.testTag("pilot-listen")) { Text("Pulsar para hablar con Jarvis") }
                OutlinedButton(onClick = { model.demo("gasolina") }) { Text("Probar texto local sin micrófono") }
                OutlinedButton(onClick = model::cancel) { Text("Cancelar conversación") }
                Text("Autorización de herramienta = permiso de política; comprueba por separado el efecto real.", color = Muted)
                journey.forEach { Text(journeyLabel(it)) }
                TextButton(onClick = model::clearVoiceJourney) { Text("Borrar etapas de voz") }
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

@Composable
fun AcousticProbePanel(state: AcousticProbeState, onStart: () -> Unit, onCancel: () -> Unit,
    onConfirm: (Long, Boolean) -> Unit) {
    DashboardCard {
        Text("Confirmación acústica guiada", modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
        Text(AcousticProbe.PHRASE)
        Text("Estado técnico: ${state.status.name}")
        Text("Observación humana: ${state.human.name}", modifier = Modifier.testTag("acoustic-human"))
        Text("Registro temporal de esta sesión. Calificación física PENDING hasta acta de dispositivo y APK. No se graba audio.", color = Muted)
        OutlinedButton(onClick = onStart, enabled = state.status != AcousticProbeStatus.SPEAKING) { Text("Reproducir frase guiada") }
        OutlinedButton(onClick = onCancel, enabled = state.status == AcousticProbeStatus.SPEAKING) { Text("Cancelar frase") }
        if (state.status == AcousticProbeStatus.COMPLETED) {
            Text("Confirma si has escuchado completa y correctamente la frase en español.")
            OutlinedButton(onClick = { onConfirm(state.attempt, true) }, enabled = state.human == HumanOutcome.PENDING,
                modifier = Modifier.testTag("acoustic-heard")) { Text("Sí, la he escuchado correctamente") }
            OutlinedButton(onClick = { onConfirm(state.attempt, false) }, enabled = state.human == HumanOutcome.PENDING,
                modifier = Modifier.testTag("acoustic-not-heard")) { Text("No se ha escuchado correctamente") }
        }
        if (state.status == AcousticProbeStatus.FAILED) Text("TTS no disponible. La frase escrita se conserva; revisa motor y voz local es-ES.", color = Amber)
    }
}

private fun journeyLabel(stage: VoiceJourneyStage) = when (stage) {
    VoiceJourneyStage.REQUEST_STARTED -> "Solicitud iniciada"
    VoiceJourneyStage.STT_LISTENING -> "STT escuchando"
    VoiceJourneyStage.STT_TRANSCRIBING -> "STT procesando voz"
    VoiceJourneyStage.TRANSCRIPT_OBTAINED -> "Transcripción obtenida (contenido oculto)"
    VoiceJourneyStage.ORCHESTRATOR_PROCESSING -> "Orquestador procesando"
    VoiceJourneyStage.TOOL_REQUESTED -> "Herramienta solicitada"
    VoiceJourneyStage.TOOL_AUTHORIZED -> "Herramienta autorizada por política"
    VoiceJourneyStage.TOOL_REJECTED -> "Herramienta rechazada por política"
    VoiceJourneyStage.TEXT_RESPONSE -> "Respuesta textual disponible en dashboard"
    VoiceJourneyStage.TTS_STARTED -> "TTS iniciado"
    VoiceJourneyStage.TTS_COMPLETED -> "TTS finalizado (callback técnico)"
    VoiceJourneyStage.PERMISSION_REQUIRED -> "Permiso o consentimiento requerido"
    VoiceJourneyStage.FAILED -> "Etapa fallida; consulta el estado de voz"
    VoiceJourneyStage.CANCELLED -> "Cancelado"
}
