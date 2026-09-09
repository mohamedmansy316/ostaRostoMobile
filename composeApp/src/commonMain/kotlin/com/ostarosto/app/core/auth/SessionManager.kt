package com.ostarosto.app.core.auth

import com.ostarosto.app.domain.model.Customer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AuthState {
    /** Startup — we haven't decided yet. */
    data object Unknown : AuthState

    data object LoggedOut : AuthState

    data class LoggedIn(val customer: Customer, val needsProfile: Boolean) : AuthState
}

/**
 * Single source of truth for "who is signed in". The UI observes [state];
 * repositories call [onSignedIn] / [onSignedOut] / [onProfileCompleted].
 */
class SessionManager(private val tokenStore: TokenStore) {

    private val _state = MutableStateFlow<AuthState>(AuthState.Unknown)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /** Called once at startup after we know whether a token exists. */
    fun bootstrap(customer: Customer?) {
        _state.value = when {
            !tokenStore.hasToken -> AuthState.LoggedOut
            customer != null -> AuthState.LoggedIn(customer, needsProfile = customer.needsProfile)
            else -> AuthState.LoggedOut
        }
    }

    fun onSignedIn(token: String, customer: Customer, needsProfile: Boolean) {
        tokenStore.save(token)
        _state.value = AuthState.LoggedIn(customer, needsProfile)
    }

    fun onProfileCompleted(customer: Customer) {
        _state.value = AuthState.LoggedIn(customer, needsProfile = false)
    }

    fun onSignedOut() {
        tokenStore.clear()
        _state.value = AuthState.LoggedOut
    }

    val currentCustomer: Customer?
        get() = (_state.value as? AuthState.LoggedIn)?.customer
}
