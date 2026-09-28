package com.ostarosto.app.core.platform

/** Result of a one-shot "where is the user right now" fetch. */
sealed interface LocationFix {
    data class Success(val latitude: Double, val longitude: Double) : LocationFix
    data object PermissionDenied : LocationFix
    data object LocationDisabled : LocationFix
    data class Error(val message: String?) : LocationFix
}

/**
 * A single current-position read — no continuous tracking, no background
 * geofencing. Requests the platform permission if not already granted.
 */
interface LocationProvider {
    suspend fun getCurrentLocation(): LocationFix
}
