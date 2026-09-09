package com.ostarosto.app.data.repository

import com.ostarosto.app.core.network.ApiClient
import com.ostarosto.app.core.network.ApiResult
import com.ostarosto.app.core.network.map
import com.ostarosto.app.data.dto.OrderDto
import com.ostarosto.app.data.dto.PlaceOrderBody
import com.ostarosto.app.data.dto.PlaceOrderResponseDto
import com.ostarosto.app.data.dto.toDomain
import com.ostarosto.app.domain.model.Order
import com.ostarosto.app.domain.model.PlaceOrderOutcome
import kotlinx.serialization.builtins.ListSerializer

class OrderRepository(private val api: ApiClient) {

    suspend fun list(page: Int = 1): ApiResult<List<Order>> =
        api.get("orders", ListSerializer(OrderDto.serializer()), query = mapOf("page" to page))
            .map { list -> list.map { it.toDomain() } }

    suspend fun detail(id: Long): ApiResult<Order> =
        api.get("orders/$id", OrderDto.serializer()).map { it.toDomain() }

    suspend fun place(body: PlaceOrderBody): ApiResult<PlaceOrderOutcome> =
        api.post("orders", PlaceOrderResponseDto.serializer(), body).map { dto ->
            if (dto.requiresPayment && dto.paymentUrl != null && dto.reference != null) {
                PlaceOrderOutcome.PaymentRequired(dto.paymentUrl, dto.reference)
            } else {
                PlaceOrderOutcome.Placed(dto.order!!.toDomain())
            }
        }

    suspend fun cancel(id: Long): ApiResult<Order> =
        api.post("orders/$id/cancel", OrderDto.serializer()).map { it.toDomain() }
}
