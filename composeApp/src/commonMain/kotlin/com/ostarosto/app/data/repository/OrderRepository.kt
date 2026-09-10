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
        when (val r = api.post("orders", PlaceOrderResponseDto.serializer(), body)) {
            is ApiResult.Success -> {
                val dto = r.value
                when {
                    dto.requiresPayment && dto.paymentUrl != null && dto.reference != null ->
                        ApiResult.Success(PlaceOrderOutcome.PaymentRequired(dto.paymentUrl, dto.reference))

                    dto.order != null ->
                        ApiResult.Success(PlaceOrderOutcome.Placed(dto.order.toDomain()))

                    // Server said no payment needed but sent no order — treat as a
                    // failed response rather than crashing on a null assertion.
                    else -> ApiResult.HttpError(200, "Malformed order response")
                }
            }
            is ApiResult.HttpError -> r
            is ApiResult.NetworkError -> r
        }

    suspend fun cancel(id: Long): ApiResult<Order> =
        api.post("orders/$id/cancel", OrderDto.serializer()).map { it.toDomain() }
}
