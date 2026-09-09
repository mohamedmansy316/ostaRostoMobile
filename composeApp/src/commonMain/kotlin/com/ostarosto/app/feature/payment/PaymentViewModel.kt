package com.ostarosto.app.feature.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ostarosto.app.core.network.ApiResult
import com.ostarosto.app.data.repository.PaymentRepository
import com.ostarosto.app.domain.model.Order
import com.ostarosto.app.domain.model.PaymentPoll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaymentUiState(
    val poll: PaymentPoll = PaymentPoll.Pending,
    val order: Order? = null,
    val checking: Boolean = false,
    val error: String? = null,
)

class PaymentViewModel(private val payments: PaymentRepository) : ViewModel() {

    private val _state = MutableStateFlow(PaymentUiState())
    val state: StateFlow<PaymentUiState> = _state.asStateFlow()

    private var polling = false

    fun startPolling(reference: String) {
        if (polling) return
        polling = true
        viewModelScope.launch {
            repeat(60) { // ~5 minutes at 5s
                if (_state.value.poll != PaymentPoll.Pending) return@launch
                check(reference)
                if (_state.value.poll != PaymentPoll.Pending) return@launch
                delay(5_000)
            }
        }
    }

    fun check(reference: String) {
        _state.update { it.copy(checking = true, error = null) }
        viewModelScope.launch {
            when (val r = payments.status(reference)) {
                is ApiResult.Success -> _state.update {
                    it.copy(checking = false, poll = r.value.poll, order = r.value.order)
                }
                is ApiResult.HttpError -> _state.update { it.copy(checking = false, error = r.message) }
                is ApiResult.NetworkError -> _state.update { it.copy(checking = false, error = r.cause.message) }
            }
        }
    }
}
