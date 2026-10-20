package ai.copilotdrive.core

import kotlin.math.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlaceCategory(val label: String, val googleType: String) {
    RESTAURANT("Restaurantes", "restaurant"), FUEL("Gasolineras", "gas_station"),
    PARKING("Aparcamientos", "parking"), CAFE("Cafeterías", "cafe"),
    POINT_OF_INTEREST("Puntos de interés", "tourist_attraction");
    companion object {
        fun fromQuery(query: String): PlaceCategory? = when (normalize(query)) {
            "restaurantes", "restaurante" -> RESTAURANT
            "gasolineras", "gasolinera" -> FUEL
            "aparcamientos", "aparcamiento", "parking" -> PARKING
            "cafeterias", "cafeteria", "cafes", "cafe" -> CAFE
            "puntos de interes", "punto de interes" -> POINT_OF_INTEREST
            else -> null
        }
    }
}

enum class LocationAvailability { AVAILABLE, PERMISSION_REQUIRED, DISABLED, UNAVAILABLE, STALE }
data class LocationSnapshot(
    val coordinates: Coordinates?, val accuracyMeters: Float?, val capturedAt: Long,
    val source: String, val availability: LocationAvailability, val maxAgeMs: Long = 30_000,
) {
    fun usableAt(now: Long): Boolean = availability == LocationAvailability.AVAILABLE &&
        coordinates?.valid() == true && capturedAt in 1..now && now - capturedAt <= maxAgeMs &&
        accuracyMeters != null && accuracyMeters.isFinite() && accuracyMeters >= 0
}

data class PlaceSearchRequest(val category: PlaceCategory, val center: LocationSnapshot, val radiusMeters: Double = 3_000.0) {
    init { require(radiusMeters in 1.0..50_000.0) }
}
data class PlaceSearchResult(
    val providerId: String?, val name: String?, val category: PlaceCategory,
    val address: String?, val coordinates: Coordinates?, val rating: Double?, val ratingCount: Int?,
    val distanceMeters: Double?, val openNow: Boolean?, val source: String,
    val capturedAt: Long, val availability: Availability,
    val attributions: List<String> = emptyList(),
) {
    fun safe(): PlaceSearchResult? {
        if (providerId.isNullOrBlank() || providerId.length > 200 || name.isNullOrBlank() ||
            coordinates?.valid() == false || rating?.let { !it.isFinite() || it !in 0.0..5.0 } == true ||
            ratingCount?.let { it < 0 } == true || capturedAt < 0 ||
            source.isBlank() || source.length > 80 || source.any { it.isISOControl() } ||
            attributions.size > 10 || attributions.any { it.length > 4_096 }) return null
        fun clean(value: String?, limit: Int) = value?.filter { !it.isISOControl() }?.take(limit)?.trim()?.takeIf { it.isNotEmpty() }
        return copy(providerId = clean(providerId, 200), name = clean(name, 100), address = clean(address, 160),
            distanceMeters = distanceMeters?.takeIf { it.isFinite() && it >= 0 })
    }
    fun destination(): String? = coordinates?.takeIf { it.valid() }?.let { "${it.latitude},${it.longitude}" }
        ?: address?.takeIf { it.isNotBlank() }
}
data class PlaceDetails(val place: PlaceSearchResult, val retrievedAt: Long)
data class PlaceSelectionState(val selectedId: String?, val selectedAt: Long, val expiresAt: Long)
data class PlacesProviderError(val code: String, override val message: String) : Exception(message)
data class PlacesState(
    val category: PlaceCategory? = null, val location: LocationSnapshot? = null,
    val results: List<PlaceSearchResult> = emptyList(), val selection: PlaceSelectionState? = null,
    val status: String = "NOT_CONFIGURED", val error: String? = null, val demo: Boolean = false,
)

fun Coordinates.valid() = latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0
fun approximateDistanceMeters(a: Coordinates, b: Coordinates): Double {
    require(a.valid() && b.valid())
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLon / 2).pow(2)
    return 12_742_000.0 * asin(sqrt(h.coerceIn(0.0, 1.0)))
}

interface LocationProvider { suspend fun current(): LocationSnapshot }
interface PlacesProvider { suspend fun searchNearby(request: PlaceSearchRequest): List<PlaceSearchResult> }

/** Session-only results; the model supplies only a category, never a search center. */
class PlacesController(
    private val locationProvider: LocationProvider, private val provider: PlacesProvider,
    private val navigation: NavigationProvider, private val clock: Clock,
    private val canQuery: () -> Boolean, private val hasLocationPermission: () -> Boolean,
    private val isDemo: () -> Boolean = { false },
    private val safeToExplore: () -> Boolean = { true },
) {
    private val mutable = MutableStateFlow(PlacesState())
    val state = mutable.asStateFlow()
    private var generation = 0L
    fun clear() { generation++; mutable.value = PlacesState(status = if (isDemo()) "DEMO_READY" else "NOT_CONFIGURED", demo = isDemo()) }
    suspend fun search(category: PlaceCategory): ToolResult {
        clear()
        val searchGeneration = generation
        mutable.value = mutable.value.copy(category = category, status = "SEARCHING")
        if (!safeToExplore()) return fail("MOTION_UNVERIFIED", "Abre Lugares y confirma que estás en un entorno detenido.")
        if (!canQuery()) return fail("CONSENT_REQUIRED", "Autoriza la consulta al proveedor de lugares desde Lugares.")
        if (!hasLocationPermission()) return fail("PERMISSION_REQUIRED", "Concede ubicación aproximada al iniciar la búsqueda desde Lugares.")
        return try {
            val location = locationProvider.current()
            mutable.value = mutable.value.copy(location = location)
            if (!location.usableAt(clock.now())) return fail("LOCATION_UNAVAILABLE", "La ubicación falta o está caducada. Reintenta la búsqueda.")
            if (!hasLocationPermission()) return fail("PERMISSION_REQUIRED", "Se revocó el permiso de ubicación.")
            val results = provider.searchNearby(PlaceSearchRequest(category, location))
                .mapNotNull { it.safe() }.filter { it.category == category }.distinctBy { it.providerId }.take(20)
                .map { place -> place.copy(distanceMeters = place.coordinates?.let { approximateDistanceMeters(location.coordinates!!, it) }) }
            if (generation != searchGeneration) return ToolResult.denied("places.search_nearby", "CANCELLED", "La búsqueda fue reemplazada o borrada.")
            if (!safeToExplore()) return fail("MOTION_UNVERIFIED", "La exploración se canceló al salir del entorno detenido.")
            if (!canQuery()) return fail("CONSENT_REQUIRED", "Se revocó la autorización de lugares.")
            if (!hasLocationPermission()) return fail("PERMISSION_REQUIRED", "Se revocó el permiso de ubicación.")
            mutable.value = mutable.value.copy(results = results, status = if (results.isEmpty()) "EMPTY" else "READY", demo = isDemo())
            ToolResult("places.search_nearby", ExecutionStatus.SUCCESS,
                if (results.isEmpty()) "No se encontraron lugares. ${if (isDemo()) "Simulación." else ""}" else
                    "${if (isDemo()) "Simulación. " else ""}${results.size} lugares encontrados. Abre Lugares para verlos y elegir uno.")
        } catch (cancelled: CancellationException) {
            if (generation == searchGeneration) mutable.value = mutable.value.copy(status = if (cancelled is TimeoutCancellationException) "TIMEOUT" else "CANCELLED",
                error = "La búsqueda se canceló o agotó el tiempo de espera.")
            throw cancelled
        }
        catch (error: PlacesProviderError) { fail(error.code, error.message) }
        catch (_: SecurityException) { fail("PERMISSION_REQUIRED", "Se revocó el permiso de ubicación.") }
        catch (_: Exception) { fail("PROVIDER_ERROR", "No se pudo consultar el proveedor de lugares.") }
    }
    private fun fail(code: String, message: String): ToolResult {
        mutable.value = mutable.value.copy(results = emptyList(), selection = null, status = code, error = message)
        return ToolResult.denied("places.search_nearby", code, message)
    }
    fun select(id: String): ToolResult {
        val found = state.value.results.singleOrNull { it.providerId == id }
            ?: return ToolResult.denied("places.select_result", "SELECTION_MISSING", "El lugar ya no está en los resultados.")
        val now = clock.now()
        mutable.value = state.value.copy(selection = PlaceSelectionState(id, now, now + 120_000))
        return ToolResult("places.select_result", ExecutionStatus.SUCCESS, "Seleccionado: ${found.name}.")
    }
    fun selected(): PlaceSearchResult? {
        val selection = state.value.selection ?: return null
        if (clock.now() > selection.expiresAt) { mutable.value = state.value.copy(selection = null); return null }
        return state.value.results.singleOrNull { it.providerId == selection.selectedId }
    }
    suspend fun open(providerName: String): ToolResult {
        if (!safeToExplore()) return ToolResult.denied("navigation.open_selected_place", "MOTION_UNVERIFIED", "Confirma entorno detenido antes de abrir un destino.")
        if (providerName !in setOf("maps", "waze")) return ToolResult.denied("navigation.open_selected_place", "INVALID_ARGUMENTS", "Navegador no válido.")
        val place = selected() ?: return ToolResult.denied("navigation.open_selected_place", "SELECTION_MISSING", "Elige de nuevo un lugar vigente.")
        val destination = place.destination() ?: return ToolResult.denied("navigation.open_selected_place", "INVALID_DESTINATION", "Este lugar no tiene destino válido.")
        return navigation.openDestination(destination, providerName)
    }
}
