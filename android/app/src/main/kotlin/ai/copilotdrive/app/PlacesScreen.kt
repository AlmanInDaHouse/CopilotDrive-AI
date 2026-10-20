package ai.copilotdrive.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
import ai.copilotdrive.app.ui.*
import ai.copilotdrive.core.*

@Composable
fun PlacesScreen(state: DashboardState, model: DashboardViewModel, onBack: () -> Unit, onSearch: (PlaceCategory) -> Unit) {
    val stopped = state.placesStationary
    var consentDialog by remember { mutableStateOf(false) }
    val places = state.places
    val selected = places.selection?.let { selection -> places.results.singleOrNull { it.providerId == selection.selectedId } }
    LazyColumn(Modifier.fillMaxSize().testTag("places-screen"), contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Lugares cercanos", style = MaterialTheme.typography.headlineMedium)
                OutlinedButton(onClick = onBack, modifier = Modifier.heightIn(min = 56.dp)) { Text("Volver") }
            }
            Text("Exploración solo en laboratorio detenido. No se comprueba el movimiento automáticamente.", color = Amber)
            Row { Checkbox(checked = stopped, onCheckedChange = model::setPlacesStationary, modifier = Modifier.testTag("places-stopped")); Text("Estoy en un entorno detenido") }
        }
        item {
            DashboardCard {
                Eyebrow("FUENTE Y PRIVACIDAD")
                Text("Ubicación: ${places.location?.availability?.name ?: "sin consultar"} · ${places.location?.source ?: "sin fuente"}")
                places.location?.let { fix -> fix.accuracyMeters?.let { Text("Precisión informada: ~${it.toInt()} m · fix ${fix.capturedAt}", color = Muted) } }
                Text("Proveedor: ${if (places.demo) "SIMULACIÓN" else if (state.placesConfigured) "Google Places" else "NOT_CONFIGURED"}")
                Text("Estado: ${places.status}", color = Muted)
                if (state.placesConfigured && !places.demo) {
                    Text("Al buscar, se enviará una ubicación aproximada a Google Places. El consentimiento de IA es independiente.", color = Muted)
                    if (!state.placesConsent) Button(onClick = { consentDialog = true }, modifier = Modifier.heightIn(min = 56.dp)) { Text("Autorizar Google Places") }
                    else OutlinedButton(onClick = { model.setPlacesConsent(false) }) { Text("Revocar autorización") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Modo de prueba · datos ficticios")
                    Switch(checked = places.demo, onCheckedChange = model::setDemoPlaces, modifier = Modifier.testTag("places-demo"))
                }
                if (!state.placesConfigured && !places.demo) Text("Configura un proveedor o activa la demostración.", color = Amber)
            }
        }
        item {
            DashboardCard {
                Eyebrow("BUSCAR")
                Text("Categoría: ${places.category?.label ?: "sin elegir"}")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlaceCategory.entries.take(3).forEach { category ->
                        Button(onClick = { onSearch(category) }, enabled = stopped && (places.demo || state.placesConfigured && state.placesConsent),
                            modifier = Modifier.weight(1f).heightIn(min = 64.dp).testTag("search-${category.name}")) { Text(category.label) }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlaceCategory.entries.drop(3).forEach { category ->
                        Button(onClick = { onSearch(category) }, enabled = stopped && (places.demo || state.placesConfigured && state.placesConsent),
                            modifier = Modifier.weight(1f).heightIn(min = 64.dp).testTag("search-${category.name}")) { Text(category.label) }
                    }
                }
                places.error?.let { Text(it, color = Amber) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { places.category?.let(onSearch) }, enabled = stopped && places.category != null) { Text("Reintentar") }
                    OutlinedButton(onClick = model::clearPlaces) { Text("Borrar resultados") }
                }
            }
        }
        itemsIndexed(places.results, key = { _, place -> place.providerId ?: place.name.orEmpty() }) { index, place ->
            DashboardCard {
                Text("${index + 1}. ${place.name ?: "Nombre no disponible"}", style = MaterialTheme.typography.titleLarge)
                place.address?.let { Text(it, color = Muted) }
                Text(buildString {
                    if (place.rating != null) append("${place.rating}/5") else append("Sin valoración")
                    place.ratingCount?.let { append(" · $it reseñas") }
                    place.distanceMeters?.let { append(" · ~${it.toInt()} m en línea recta") }
                }, color = Muted)
                place.openNow?.let { Text(if (it) "Abierto según proveedor" else "Cerrado según proveedor", color = Muted) }
                if (place.source == "Google Maps") Text("Google Maps", color = Color.White, fontSize = 14.sp)
                else Text(place.source, color = Amber)
                Text(place.availability.name, color = Muted)
                place.attributions.forEach { attribution -> AndroidView(factory = { context -> TextView(context).apply {
                    setTextColor(android.graphics.Color.WHITE); textSize = 14f; movementMethod = LinkMovementMethod.getInstance()
                } }, update = { view -> view.text = Html.fromHtml(attribution, Html.FROM_HTML_MODE_COMPACT) }) }
                OutlinedButton(onClick = { place.providerId?.let(model::selectPlace) }, enabled = stopped,
                    modifier = Modifier.heightIn(min = 56.dp).testTag("select-place-$index")) {
                    Text(if (selected?.providerId == place.providerId) "Seleccionado" else "Elegir destino")
                }
            }
        }
        item {
            DashboardCard {
                Eyebrow("DESTINO")
                Text("Selección: ${selected?.name ?: "ninguna"}")
                Text("Al abrir una app de navegación, ella calculará la ruta. Aquí no se conoce la ETA.", color = Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { model.openSelectedPlace("maps") }, enabled = stopped && selected != null,
                        modifier = Modifier.heightIn(min = 64.dp).testTag("open-selected-maps")) { Text("Abrir en Maps") }
                    Button(onClick = { model.openSelectedPlace("waze") }, enabled = stopped && selected != null,
                        modifier = Modifier.heightIn(min = 64.dp)) { Text("Abrir en Waze") }
                }
                Text(state.notice, color = Muted)
            }
        }
    }
    if (consentDialog) AlertDialog(onDismissRequest = { consentDialog = false },
        title = { Text("Autorizar Google Places") },
        text = { Text("Una búsqueda enviará la ubicación aproximada y categoría a Google Places. Puede consumir cuota facturable. No se enviará al proveedor IA.") },
        confirmButton = { TextButton(onClick = { model.setPlacesConsent(true); consentDialog = false }) { Text("Autorizar") } },
        dismissButton = { TextButton(onClick = { consentDialog = false }) { Text("Cancelar") } })
}
