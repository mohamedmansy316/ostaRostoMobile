package com.ostarosto.app.data.repository

import com.ostarosto.app.core.auth.SessionManager
import com.ostarosto.app.core.network.ApiClient
import com.ostarosto.app.core.network.ApiResult
import com.ostarosto.app.core.network.map
import com.ostarosto.app.data.dto.CompleteProfileBody
import com.ostarosto.app.data.dto.CustomerDto
import com.ostarosto.app.data.dto.RequestOtpBody
import com.ostarosto.app.data.dto.VerifyOtpBody
import com.ostarosto.app.data.dto.VerifyOtpDataDto
import com.ostarosto.app.data.dto.toDomain
import com.ostarosto.app.domain.model.Customer
import kotlinx.serialization.builtins.serializer

class AuthRepository(
    private val api: ApiClient,
    private val session: SessionManager,
) {

    suspend fun requestOtp(dialCode: Int, phone: String): ApiResult<Unit> =
        api.postUnit("auth/otp/request", RequestOtpBody(dialCode, phone))

    suspend fun verifyOtp(
        dialCode: Int,
        phone: String,
        otp: String,
        deviceName: String?,
    ): ApiResult<Customer> {
        val result = api.post(
            "auth/otp/verify",
            VerifyOtpDataDto.serializer(),
            VerifyOtpBody(dialCode, phone, otp, deviceName),
        )
        return result.map { dto ->
            val customer = dto.customer.toDomain()
            session.onSignedIn(dto.token, customer, dto.needsProfile)
            customer
        }
    }

    suspend fun completeProfile(name: String, email: String?): ApiResult<Customer> {
        val result = api.post("auth/complete-profile", CustomerDto.serializer(), CompleteProfileBody(name, email))
        return result.map { dto ->
            val customer = dto.toDomain()
            session.onProfileCompleted(customer)
            customer
        }
    }

    suspend fun me(): ApiResult<Customer> =
        api.get("auth/me", CustomerDto.serializer()).map { it.toDomain() }

    suspend fun logout(): ApiResult<Unit> {
        val result = api.postUnit("auth/logout")
        session.onSignedOut()
        return result
    }
}
