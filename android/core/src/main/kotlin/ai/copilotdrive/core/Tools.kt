package ai.copilotdrive.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

data class ToolDefinition(
    val id: String, val capability: String, val arguments: Set<String> = emptySet(),
    val requiresConfirmation: Boolean = false, val timeoutMs: Long = 3_000,
    val optionalArguments: Set<String> = emptySet(),
)

class PolicyEngine(private val capabilities: CapabilityRegistry) {
    fun evaluate(definition: ToolDefinition, request: ToolRequest, confirmedNumber: String?): ToolResult? {
        if (!request.arguments.keys.containsAll(definition.arguments) ||
            !((definition.arguments + definition.optionalArguments).containsAll(request.arguments.keys)) ||
            request.arguments.values.any { it.isBlank() || it.length > 200 }) {
            return ToolResult.denied(request.toolId, "INVALID_ARGUMENTS", "Parámetros de herramienta no válidos.")
        }
        if (!capabilities.available(definition.capability)) {
            return ToolResult.denied(request.toolId, capabilities.get(definition.capability).name, "Función no disponible: ${definition.capability}. Revisa permisos y configuración.")
        }
        if (definition.requiresConfirmation && (confirmedNumber == null || confirmedNumber != request.arguments["number"])) {
            return ToolResult(request.toolId, ExecutionStatus.CONFIRMATION_REQUIRED, "Confirma primero el contacto y número.", "CONFIRMATION_REQUIRED")
        }
        return null
    }
}

class ToolRegistry(
    private val policy: PolicyEngine,
    private val vehicle: VehicleContextEngine,
    private val media: MediaSourceAdapter,
    private val navigation: NavigationProvider,
    private val contacts: ContactProvider,
    private val places: PlacesController? = null,
) {
    fun placeResults(): List<PlaceSearchResult> = places?.state?.value?.results.orEmpty()
    fun selectedPlaceId(): String? = places?.selected()?.providerId
    val definitions = listOf(
        ToolDefinition("vehicle.get_fuel", "vehicle.fuel"),
        ToolDefinition("vehicle.get_range", "vehicle.fuel"),
        ToolDefinition("vehicle.get_temperature", "vehicle.temperature"),
        ToolDefinition("vehicle.get_snapshot", "vehicle.snapshot"),
        ToolDefinition("media.get_current", "media.session_access"),
        ToolDefinition("media.play", "media.playback_control"),
        ToolDefinition("media.pause", "media.playback_control", optionalArguments = setOf("sourceApp")),
        ToolDefinition("media.next", "media.playback_control"),
        ToolDefinition("media.previous", "media.playback_control"),
        ToolDefinition("navigation.open_destination", "navigation.available", setOf("destination", "provider")),
        ToolDefinition("contacts.search", "contacts.access", setOf("name")),
        ToolDefinition("phone.open_dialer", "phone.dialer", setOf("number"), requiresConfirmation = true),
        ToolDefinition("places.search_nearby", "places.available", setOf("query"), timeoutMs = 15_000),
        ToolDefinition("places.select_result", "places.available", setOf("id")),
        ToolDefinition("navigation.open_selected_place", "navigation.available", setOf("provider")),
    ).associateBy { it.id }

    suspend fun execute(request: ToolRequest, confirmedNumber: String? = null): ToolResult {
        val definition = definitions[request.toolId]
            ?: return ToolResult.denied(request.toolId, "UNKNOWN_TOOL", "Herramienta no permitida.")
        policy.evaluate(definition, request, confirmedNumber)?.let { return it }
        return try {
            withTimeout(definition.timeoutMs) { dispatch(request) }
        } catch (_: TimeoutCancellationException) {
            ToolResult(request.toolId, ExecutionStatus.FAILED, "La operación ha agotado el tiempo de espera.", "TIMEOUT")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SecurityException) {
            ToolResult.denied(request.toolId, "PERMISSION_REQUIRED", "Permiso denegado o revocado.")
        } catch (_: IllegalArgumentException) {
            ToolResult.denied(request.toolId, "INVALID_ARGUMENTS", "Destino o parámetros no válidos.")
        } catch (_: Exception) {
            ToolResult(request.toolId, ExecutionStatus.FAILED, "No se pudo completar la operación.", "PROVIDER_ERROR")
        }
    }

    private suspend fun dispatch(request: ToolRequest): ToolResult = when (request.toolId) {
        "vehicle.get_fuel", "vehicle.get_range", "vehicle.get_temperature", "vehicle.get_snapshot" -> {
            val snapshot = vehicle.snapshot()
            val prefix = if (snapshot.simulationMode) "Simulación. " else "Fuente ${snapshot.source}. "
            val message = when (request.toolId) {
                "vehicle.get_fuel" -> describe("Combustible", snapshot.fuelLevelPercent)
                "vehicle.get_range" -> if (snapshot.vehicleRangeKm.availability in setOf(Availability.AVAILABLE, Availability.SIMULATED))
                    describe("Autonomía", snapshot.vehicleRangeKm) else "No tengo una estimación fiable de autonomía para ese vehículo."
                "vehicle.get_temperature" -> describe("Refrigerante", snapshot.coolantTemperatureC)
                else -> describe("Combustible", snapshot.fuelLevelPercent) + " " +
                    describe("Refrigerante", snapshot.coolantTemperatureC) + " " + describe("Autonomía", snapshot.vehicleRangeKm)
            }
            ToolResult(request.toolId, ExecutionStatus.SUCCESS, prefix + message)
        }
        "media.get_current" -> {
            val playback = media.state.value
            ToolResult(request.toolId, ExecutionStatus.SUCCESS,
                (if (playback.simulated) "Simulación. " else "") + (playback.title ?: "Sin reproducción disponible."))
        }
        "media.play", "media.pause", "media.next", "media.previous" -> {
            val control = MediaControl.valueOf(request.toolId.substringAfter('.').uppercase())
            val expectedApp = request.arguments["sourceApp"]
            if (expectedApp != null && media.state.value.sourceApp != expectedApp) {
                ToolResult.denied(request.toolId, "WRONG_SESSION", "La sesión seleccionada no es la aplicación solicitada. Elígela en Ajustes.")
            } else if (control !in media.state.value.supportedControls) {
                ToolResult.denied(request.toolId, "UNSUPPORTED", "El reproductor no ofrece ese control.")
            } else media.command(control)
        }
        "navigation.open_destination" -> {
            val destination = request.arguments.getValue("destination")
            val provider = request.arguments.getValue("provider")
            NavigationUris.validate(destination)
            require(provider in setOf("maps", "waze"))
            navigation.openDestination(destination, provider)
        }
        "contacts.search" -> {
            val found = contacts.search(request.arguments.getValue("name")).distinctBy { it.number }.take(5)
            ToolResult(request.toolId, ExecutionStatus.SUCCESS,
                if (found.isEmpty()) "No se encontraron contactos." else "Contactos encontrados.", contacts = found)
        }
        "phone.open_dialer" -> {
            val number = request.arguments.getValue("number")
            require(NavigationUris.validPhone(number))
            contacts.openDialer(number)
        }
        "places.search_nearby" -> {
            val category = PlaceCategory.fromQuery(request.arguments.getValue("query"))
                ?: return ToolResult.denied(request.toolId, "INVALID_CATEGORY", "Categoría de lugar no válida.")
            places?.search(category) ?: ToolResult.denied(request.toolId, "NOT_CONFIGURED", "Proveedor de lugares no configurado.")
        }
        "places.select_result" -> places?.select(request.arguments.getValue("id"))
            ?: ToolResult.denied(request.toolId, "NOT_CONFIGURED", "Proveedor de lugares no configurado.")
        "navigation.open_selected_place" -> places?.open(request.arguments.getValue("provider"))
            ?: ToolResult.denied(request.toolId, "NOT_CONFIGURED", "Proveedor de lugares no configurado.")
        else -> ToolResult.denied(request.toolId, "NOT_CONFIGURED", "El proveedor de lugares no está configurado.")
    }

    private fun describe(label: String, metric: Metric): String = when (metric.availability) {
        Availability.AVAILABLE, Availability.SIMULATED -> "$label: ${formatNumber(metric.value!!)} ${metric.unit}."
        Availability.STALE -> "$label: dato caducado; no disponible actualmente."
        Availability.DISCONNECTED -> "$label: no disponible, fuente desconectada."
        Availability.UNAVAILABLE -> "$label: no disponible."
    }
}

fun formatNumber(number: Double): String = if (number == number.toLong().toDouble()) number.toLong().toString() else "%.1f".format(java.util.Locale.ROOT, number)
