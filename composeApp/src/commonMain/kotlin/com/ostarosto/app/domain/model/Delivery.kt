package com.ostarosto.app.domain.model

data class DeliveryAvailability(
    val available: Boolean,
    val zoneId: Int?,
    val zoneName: String?,
    val branchId: Long?,
    val branchName: String?,
    val deliveryFee: Double,
    val distanceKm: Double?,
    val estimatedTime: String?,
)

/**
 * One browsable zone (name + flat delivery fee) for a branch's delivery area —
 * informational only. Zones are polygons server-side, so there is no single
 * "center" point to select as an order's coordinates; an order's actual
 * lat/lng always comes from a real pin (map pick or saved address).
 */
data class DeliveryZone(
    val id: Int,
    val name: String,
    val deliveryFee: Double,
    val branchId: Long?,
)
