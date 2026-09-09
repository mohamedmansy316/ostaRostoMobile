package com.ostarosto.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ostarosto.app.core.network.ApiResult
import com.ostarosto.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthStep { Phone, Otp, Profile, Done }

data class AuthUiState(
    val step: AuthStep = AuthStep.Phone,
    val dialCode: Int = 20,
    val phone: String = "",
    val otp: String = "",
    val name: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val resendCooldown: Int = 0,
)

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onDialCode(code: Int) = _state.update { it.copy(dialCode = code, error = null) }
    fun onPhone(value: String) = _state.update { it.copy(phone = value.filter(Char::isDigit), error = null) }
    fun onOtp(value: String) = _state.update { it.copy(otp = value.filter(Char::isDigit).take(6), error = null) }
    fun onName(value: String) = _state.update { it.copy(name = value, error = null) }

    fun requestOtp() = launchGuarded {
        when (val r = repo.requestOtp(_state.value.dialCode, _state.value.phone)) {
            is ApiResult.Success -> _state.update { it.copy(step = AuthStep.Otp, loading = false) }
            else -> fail(r)
        }
    }

    fun verifyOtp() = launchGuarded {
        val s = _state.value
        when (val r = repo.verifyOtp(s.dialCode, s.phone, s.otp, deviceName = null)) {
            is ApiResult.Success ->
                _state.update {
                    it.copy(
                        loading = false,
                        step = if (r.value.needsProfile) AuthStep.Profile else AuthStep.Done,
                    )
                }
            else -> fail(r)
        }
    }

    fun completeProfile() = launchGuarded {
        when (val r = repo.completeProfile(_state.value.name.trim(), email = null)) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, step = AuthStep.Done) }
            else -> fail(r)
        }
    }

    private inline fun launchGuarded(crossinline block: suspend () -> Unit) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch { block() }
    }

    private fun fail(result: ApiResult<*>) {
        val message = when (result) {
            is ApiResult.HttpError -> result.message
            is ApiResult.NetworkError -> result.cause.message ?: "Network error"
            is ApiResult.Success -> return
        }
        _state.update { it.copy(loading = false, error = message) }
    }
}
