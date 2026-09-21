package com.ostarosto.app.data.repository

import com.ostarosto.app.core.network.ApiClient
import com.ostarosto.app.core.network.ApiResult
import com.ostarosto.app.core.network.map
import com.ostarosto.app.data.dto.DeliveryCheckDto
import com.ostarosto.app.data.dto.toDomain
import com.ostarosto.app.domain.model.DeliveryAvailability

class DeliveryRepository(private val api: ApiClient) {

    suspend fun check(lat: Double, lng: Double, branchId: String?): ApiResult<DeliveryAvailability> =
        api.get(
            "delivery/check",
            DeliveryCheckDto.serializer(),
            query = mapOf("lat" to lat, "lng" to lng, "branch_id" to branchId),
        ).map { it.toDomain() }
}
