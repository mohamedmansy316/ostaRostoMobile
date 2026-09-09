package com.ostarosto.app.data.repository

import com.ostarosto.app.core.network.ApiClient
import com.ostarosto.app.core.network.ApiResult
import com.ostarosto.app.core.network.map
import com.ostarosto.app.data.dto.PaymentStatusDto
import com.ostarosto.app.data.dto.toDomain
import com.ostarosto.app.domain.model.PaymentPoll
import com.ostarosto.app.domain.model.PaymentResult

class PaymentRepository(private val api: ApiClient) {

    suspend fun status(reference: String): ApiResult<PaymentResult> =
        api.get("payments/$reference", PaymentStatusDto.serializer()).map { dto ->
            val poll = when (dto.status.lowercase()) {
                "paid" -> PaymentPoll.Paid
                "failed" -> PaymentPoll.Failed
                else -> PaymentPoll.Pending
            }
            PaymentResult(poll, dto.order?.toDomain())
        }
}
