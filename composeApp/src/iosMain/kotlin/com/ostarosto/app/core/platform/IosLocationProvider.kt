package com.ostarosto.app.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * `CLLocationManager.delegate` is a *weak* reference, so the delegate must be held
 * strongly somewhere or it is deallocated before it can call back — this class is
 * itself the delegate, and is a Koin singleton kept alive for the app's lifetime.
 */
@OptIn(ExperimentalForeignApi::class)
class IosLocationProvider : NSObject(), LocationProvider, CLLocationManagerDelegateProtocol {

    private val manager = CLLocationManager().apply { delegate = this@IosLocationProvider }

    private var authContinuation: CancellableContinuation<Boolean>? = null
    private var locationContinuation: CancellableContinuation<LocationFix>? = null

    override suspend fun getCurrentLocation(): LocationFix {
        val authorized = when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedAlways, kCLAuthorizationStatusAuthorizedWhenInUse -> true
            kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted -> false
            else -> awaitAuthorization()
        }
        if (!authorized) return LocationFix.PermissionDenied
        return awaitLocation()
    }

    private suspend fun awaitAuthorization(): Boolean = suspendCancellableCoroutine { cont ->
        authContinuation = cont
        manager.requestWhenInUseAuthorization()
    }

    private suspend fun awaitLocation(): LocationFix = suspendCancellableCoroutine { cont ->
        locationContinuation = cont
        manager.requestLocation()
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        val cont = authContinuation ?: return
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusNotDetermined -> return
            kCLAuthorizationStatusAuthorizedAlways, kCLAuthorizationStatusAuthorizedWhenInUse -> {
                authContinuation = null
                if (cont.isActive) cont.resume(true)
            }
            else -> {
                authContinuation = null
                if (cont.isActive) cont.resume(false)
            }
        }
    }

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val cont = locationContinuation ?: return
        val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
        locationContinuation = null
        val fix = location.coordinate.useContents { LocationFix.Success(latitude, longitude) }
        if (cont.isActive) cont.resume(fix)
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        val cont = locationContinuation ?: return
        locationContinuation = null
        if (cont.isActive) cont.resume(LocationFix.Error(didFailWithError.localizedDescription))
    }
}
