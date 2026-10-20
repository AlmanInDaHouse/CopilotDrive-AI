package ai.copilotdrive.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.copilotdrive.app.DashboardState
import ai.copilotdrive.core.*

@Composable
fun DashboardEditor(state: DashboardState, customization: DashboardCustomizationState,
    onChange: (DashboardEditSession.() -> DashboardEditSession) -> Unit, onSave: () -> Unit,
    onCancel: () -> Unit, onRevoke: () -> Unit, onSettings: () -> Unit, onListen: () -> Unit,
    onVoiceCancel: () -> Unit, onDemo: (String) -> Unit) {
    val draft = customization.edit?.draft ?: return
    var preview by remember { mutableStateOf(false) }
    Scaffold(bottomBar = { EssentialControls(state, onSettings, onListen, onVoiceCancel, onDemo) }) { insets ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(insets)) {
            val columns = dashboardColumns(maxWidth.value, LocalDensity.current.fontScale)
            val projected = remember(draft, columns) { LayoutEngine.project(draft, columns) }
            LazyColumn(Modifier.fillMaxSize().testTag("dashboard-editor"), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Eyebrow("LABORATORIO / PERSONALIZAR")
                    Text("Tu dashboard", fontSize = 28.sp)
                    Text("Entorno de prueba detenido confirmado. Esto no verifica que el vehículo esté aparcado. Confirmación válida hasta 2 minutos.", color = Amber)
                    customization.error?.let { Text(it, color = Amber, modifier = Modifier.testTag("layout-error")) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onSave, enabled = !customization.saving, modifier = Modifier.heightIn(min = 56.dp).testTag("save-layout")) { Text(if (customization.saving) "Guardando…" else "Guardar") }
                        OutlinedButton(onClick = onCancel, enabled = !customization.saving, modifier = Modifier.heightIn(min = 56.dp).testTag("cancel-layout")) { Text("Cancelar") }
                    }
                    OutlinedButton(onClick = { preview = !preview }, modifier = Modifier.heightIn(min = 56.dp).testTag("preview-layout")) { Text(if (preview) "Volver al editor" else "Vista previa") }
                    TextButton(onClick = onRevoke, modifier = Modifier.heightIn(min = 48.dp).testTag("revoke-layout-lab")) { Text("Revocar laboratorio y salir") }
                }
                if (preview) {
                    items(projected.groupBy { it.placement.row }.values.toList(), key = { it.joinToString { w -> w.id } }) { row ->
                        WidgetRow(row, columns, state, {}, {}, {}, {}, {}, preview = true)
                    }
                } else {
                    item { Text("Presets", fontSize = 22.sp) }
                    items(DashboardPreset.entries, key = { "preset-$it" }) { preset ->
                        OutlinedButton(onClick = { onChange { preset(preset) } }, enabled = !customization.saving,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("preset-$preset")) { Text(presetLabel(preset)) }
                    }
                    item {
                        OutlinedButton(onClick = { onChange { restore() } }, enabled = !customization.saving,
                            modifier = Modifier.heightIn(min = 56.dp).testTag("restore-layout")) { Text("Restaurar distribución predeterminada") }
                        Text("Tarjetas · orden de lectura", fontSize = 22.sp)
                    }
                    items(draft.widgets, key = { it.id }) { widget ->
                        DashboardCard(Modifier.fillMaxWidth().testTag("edit-${widget.id}")) {
                            val definition = WidgetRegistry.definition(widget.type)
                            Eyebrow(definition.title)
                            Text("${if (widget.visibility == WidgetVisibility.VISIBLE) "Visible" else "Oculta"} · ${sizeLabel(widget.size)} · ${widget.placement.column}, ${widget.placement.row}", color = Muted)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onChange { move(widget.id, -1) } }, enabled = !customization.saving && draft.widgets.first() != widget,
                                    modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("move-up-${widget.id}")) { Text("Subir") }
                                OutlinedButton(onClick = { onChange { move(widget.id, 1) } }, enabled = !customization.saving && draft.widgets.last() != widget,
                                    modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("move-down-${widget.id}")) { Text("Bajar") }
                            }
                            definition.sizes.forEach { size ->
                                OutlinedButton(onClick = { onChange { resize(widget.id, size) } }, enabled = !customization.saving && size != widget.size,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("resize-${widget.id}-$size")) { Text(sizeLabel(size)) }
                            }
                            val canHide = widget.visibility == WidgetVisibility.HIDDEN || draft.widgets.count { it.visibility == WidgetVisibility.VISIBLE } > 1
                            OutlinedButton(onClick = { onChange { if (widget.visibility == WidgetVisibility.VISIBLE) hide(widget.id) else add(widget.type) } },
                                enabled = !customization.saving && canHide, modifier = Modifier.heightIn(min = 56.dp).testTag("toggle-${widget.id}")) {
                                Text(if (widget.visibility == WidgetVisibility.VISIBLE) "Ocultar" else "Mostrar")
                            }
                        }
                    }
                    item { Text("Añadir tarjeta", fontSize = 22.sp) }
                    items(WidgetRegistry.definitions.filter { d -> draft.widgets.none { it.type == d.type } }, key = { "add-${it.id}" }) { definition ->
                        OutlinedButton(onClick = { onChange { add(definition.type) } }, enabled = !customization.saving,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("add-${definition.id}")) { Text("Añadir ${definition.title}") }
                    }
                }
            }
        }
    }
}

fun presetLabel(preset: DashboardPreset) = when (preset) {
    DashboardPreset.CLASSIC -> "Classic · vehículo, audio, navegación y Jarvis"
    DashboardPreset.DRIVER -> "Driver · vehículo destacado"
    DashboardPreset.MEDIA -> "Media · multimedia destacada"
    DashboardPreset.LABORATORY -> "Laboratory · OBD y diagnóstico"
}
fun sizeLabel(size: WidgetSize) = when (size) {
    WidgetSize.COMPACT -> "Compacta · 1 columna"
    WidgetSize.EXPANDED -> "Expandida · 2 columnas"
    WidgetSize.WIDE -> "Panorámica · 4 columnas"
}
