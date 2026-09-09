package com.ostarosto.app

import com.ostarosto.app.core.auth.AuthState
import com.ostarosto.app.core.network.ApiResult
import com.ostarosto.app.data.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AuthRepositoryTest {

    @Test
    fun verifyOtp_stores_token_and_flips_session_to_logged_in() = runTest {
        val scope = testApi(
            ok(pathIs("/auth/otp/verify"), """
                {"success":true,"message":"ok","data":{
                  "token":"1|abcdef",
                  "customer":{"id":7,"name":"Customer","dial_code":20,"phone":"1001234567","email":null,"loyalty_points":0,"needs_profile":true},
                  "needs_profile":true
                }}
            """.trimIndent()),
        )
        val repo = AuthRepository(scope.api, scope.session)

        val result = repo.verifyOtp(20, "1001234567", "123456", deviceName = "test")

        assertIs<ApiResult.Success<*>>(result)
        assertEquals("1|abcdef", scope.tokenStore.peek())
        val state = scope.session.state.value
        assertIs<AuthState.LoggedIn>(state)
        assertTrue(state.needsProfile)
    }

    @Test
    fun verifyOtp_maps_a_422_to_http_error_with_field_messages() = runTest {
        val scope = testApi(
            status(422, pathIs("/auth/otp/verify"), """
                {"success":false,"message":"Invalid verification code.","errors":{"otp":["Invalid verification code."]}}
            """.trimIndent()),
        )
        val repo = AuthRepository(scope.api, scope.session)

        val result = repo.verifyOtp(20, "1001234567", "000000", deviceName = null)

        assertIs<ApiResult.HttpError>(result)
        assertEquals(422, result.status)
        assertEquals(listOf("Invalid verification code."), result.fieldErrors["otp"])
    }

    @Test
    fun completeProfile_marks_session_as_no_longer_needing_profile() = runTest {
        val scope = testApi(
            ok(pathIs("/auth/otp/verify"), """
                {"success":true,"data":{"token":"1|t","customer":{"id":1,"name":"Customer","dial_code":20,"phone":"1000000000","loyalty_points":0,"needs_profile":true},"needs_profile":true}}
            """.trimIndent()),
            ok(pathIs("/auth/complete-profile"), """
                {"success":true,"data":{"id":1,"name":"Mona","dial_code":20,"phone":"1000000000","loyalty_points":0,"needs_profile":false}}
            """.trimIndent()),
        )
        val repo = AuthRepository(scope.api, scope.session)
        repo.verifyOtp(20, "1000000000", "111111", null)

        val result = repo.completeProfile("Mona", email = null)

        assertIs<ApiResult.Success<*>>(result)
        val state = scope.session.state.value
        assertIs<AuthState.LoggedIn>(state)
        assertEquals(false, state.needsProfile)
        assertEquals("Mona", state.customer.name)
    }

    @Test
    fun logout_clears_the_token_even_though_the_call_succeeds() = runTest {
        val scope = testApi(
            ok(pathIs("/auth/otp/verify"), """
                {"success":true,"data":{"token":"1|zzz","customer":{"id":1,"name":"A","dial_code":20,"phone":"1000000000","loyalty_points":0,"needs_profile":false},"needs_profile":false}}
            """.trimIndent()),
            ok(pathIs("/auth/logout"), """{"success":true,"data":null}"""),
        )
        val repo = AuthRepository(scope.api, scope.session)
        repo.verifyOtp(20, "1000000000", "111111", null)

        repo.logout()

        assertEquals(null, scope.tokenStore.peek())
        assertIs<AuthState.LoggedOut>(scope.session.state.value)
    }
}
