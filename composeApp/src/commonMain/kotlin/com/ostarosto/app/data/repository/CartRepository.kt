package com.ostarosto.app.data.repository

import com.ostarosto.app.core.network.ApiClient
import com.ostarosto.app.core.network.ApiResult
import com.ostarosto.app.core.network.map
import com.ostarosto.app.data.dto.CartTotalsBody
import com.ostarosto.app.data.dto.CartTotalsDto
import com.ostarosto.app.data.dto.CartValidationDto
import com.ostarosto.app.data.dto.PaymentMethodDto
import com.ostarosto.app.data.dto.ValidateCartBody
import com.ostarosto.app.data.dto.toDomain
import com.ostarosto.app.domain.model.CartTotals
import com.ostarosto.app.domain.model.CartValidation
import com.ostarosto.app.domain.model.PaymentMethod
import kotlinx.serialization.builtins.ListSerializer

class CartRepository(private val api: ApiClient) {

    suspend fun totals(body: CartTotalsBody): ApiResult<CartTotals> =
        api.post("cart/totals", CartTotalsDto.serializer(), body).map { it.toDomain() }

    suspend fun validate(body: ValidateCartBody): ApiResult<CartValidation> =
        api.post("cart/validate", CartValidationDto.serializer(), body).map { it.toDomain() }

    suspend fun paymentMethods(branchId: String? = null): ApiResult<List<PaymentMethod>> =
        api.get(
            "payment-methods",
            ListSerializer(PaymentMethodDto.serializer()),
            query = mapOf("branch_id" to branchId),
        ).map { list -> list.map { it.toDomain() } }
}
