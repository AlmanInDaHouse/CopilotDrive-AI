package ai.copilotdrive.app.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import ai.copilotdrive.core.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place as GooglePlace
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidLocationProvider(private val context: Context) : LocationProvider {
    fun permitted() = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // Permission is checked immediately below and may still be revoked; PlacesController handles SecurityException.
    @SuppressLint("MissingPermission")
    override suspend fun current(): LocationSnapshot {
        if (!permitted()) return LocationSnapshot(null, null, 0, "android", LocationAvailability.PERMISSION_REQUIRED)
        val manager = context.getSystemService(LocationManager::class.java)
        val provider = when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return LocationSnapshot(null, null, 0, "android", LocationAvailability.DISABLED)
        }
        val fix = suspendCancellableCoroutine<Location?> { continuation ->
            if (Build.VERSION.SDK_INT >= 30) {
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                manager.getCurrentLocation(provider, signal, context.mainExecutor) { if (continuation.isActive) continuation.resume(it) }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        manager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(location)
                    }
                    override fun onProviderDisabled(provider: String) {
                        manager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
                continuation.invokeOnCancellation { manager.removeUpdates(listener) }
                manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            }
        } ?: return LocationSnapshot(null, null, 0, provider, LocationAvailability.UNAVAILABLE)
        return LocationSnapshot(Coordinates(fix.latitude, fix.longitude), if (fix.hasAccuracy()) fix.accuracy else null,
            fix.time, provider, LocationAvailability.AVAILABLE)
    }
}

class GooglePlacesProvider(private val context: Context, private val key: String, private val clock: Clock) : PlacesProvider {
    override suspend fun searchNearby(request: PlaceSearchRequest): List<PlaceSearchResult> {
        if (key.isBlank()) throw PlacesProviderError("NOT_CONFIGURED", "Configura una clave Android restringida para Google Places.")
        if (!request.center.usableAt(clock.now())) throw PlacesProviderError("LOCATION_UNAVAILABLE", "Ubicación caducada.")
        if (!Places.isInitialized()) Places.initializeWithNewPlacesApiEnabled(context.applicationContext, key)
        val center = request.center.coordinates!!
        val fields = listOf(GooglePlace.Field.ID, GooglePlace.Field.DISPLAY_NAME, GooglePlace.Field.FORMATTED_ADDRESS,
            GooglePlace.Field.LOCATION, GooglePlace.Field.RATING, GooglePlace.Field.USER_RATING_COUNT)
        val cancellation = CancellationTokenSource()
        val query = SearchNearbyRequest.builder(CircularBounds.newInstance(LatLng(center.latitude, center.longitude), request.radiusMeters), fields)
            .setIncludedTypes(listOf(request.category.googleType)).setMaxResultCount(10)
            .setCancellationToken(cancellation.token).build()
        val response = try {
            suspendCancellableCoroutine<com.google.android.libraries.places.api.net.SearchNearbyResponse> { continuation ->
                // The SDK may still bill a request already sent when cancellation is requested.
                continuation.invokeOnCancellation { cancellation.cancel() }
                Places.createClient(context).searchNearby(query)
                    .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
                    .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
            }
        } catch (error: ApiException) {
            throw PlacesProviderError("GOOGLE_${error.statusCode}", "Google Places rechazó la consulta (${error.statusCode}).")
        }
        return response.places.map { place ->
            PlaceSearchResult(place.id, place.displayName, request.category, place.formattedAddress,
                place.location?.let { Coordinates(it.latitude, it.longitude) }, place.rating, place.userRatingCount,
                null, null, "Google Maps", clock.now(), Availability.AVAILABLE,
                place.attributions.orEmpty())
        }
    }
}

/** Clearly labelled, deterministic demonstration. It never contacts an external service. */
class DemoPlacesProvider(private val clock: Clock) : PlacesProvider {
    override suspend fun searchNearby(request: PlaceSearchRequest): List<PlaceSearchResult> {
        val center = request.center.coordinates ?: return emptyList()
        return listOf(1, 2, 3).map { index ->
            PlaceSearchResult("demo-$index", "${request.category.label} de prueba $index", request.category, null,
                Coordinates(center.latitude + index * 0.002, center.longitude + index * 0.002), null, null,
                null, null, "SIMULACIÓN · datos ficticios", clock.now(), Availability.SIMULATED)
        }
    }
}
