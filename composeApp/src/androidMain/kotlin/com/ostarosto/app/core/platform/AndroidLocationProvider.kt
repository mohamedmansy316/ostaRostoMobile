package com.ostarosto.app.core.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

class AndroidLocationProvider(private val context: Context) : LocationProvider {

    override suspend fun getCurrentLocation(): LocationFix {
        if (!hasPermission()) {
            val granted = requestPermission()
            if (!granted) return LocationFix.PermissionDenied
        }

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return LocationFix.Error("Location service unavailable")

        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        } ?: return LocationFix.LocationDisabled

        return withTimeoutOrNull(15_000L) {
            suspendCancellableCoroutine { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        manager.removeUpdates(this)
                        if (cont.isActive) cont.resumeWith(Result.success(LocationFix.Success(location.latitude, location.longitude)))
                    }
                }
                cont.invokeOnCancellation { manager.removeUpdates(listener) }
                try {
                    manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                } catch (e: SecurityException) {
                    if (cont.isActive) cont.resumeWith(Result.success(LocationFix.PermissionDenied))
                }
            }
        } ?: LocationFix.Error("Timed out waiting for a location fix")
    }

    private fun hasPermission(): Boolean = LOCATION_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun requestPermission(): Boolean {
        LocationPermissionBridge.request(LOCATION_PERMISSIONS)
        return LocationPermissionBridge.result.first()
    }
}
