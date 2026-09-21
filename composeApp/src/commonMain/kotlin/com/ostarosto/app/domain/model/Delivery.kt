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
