package com.ostarosto.app.feature.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ostarosto.app.core.network.ApiResult
import com.ostarosto.app.data.repository.OrderRepository
import com.ostarosto.app.domain.model.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrdersUiState(
    val loading: Boolean = true,
    val orders: List<Order> = emptyList(),
    val selected: Order? = null,
    val error: String? = null,
)

class OrdersViewModel(private val repo: OrderRepository) : ViewModel() {

    private val _state = MutableStateFlow(OrdersUiState())
    val state: StateFlow<OrdersUiState> = _state.asStateFlow()

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.list()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, orders = r.value) }
                is ApiResult.HttpError -> _state.update { it.copy(loading = false, error = r.message) }
                is ApiResult.NetworkError -> _state.update { it.copy(loading = false, error = r.cause.message) }
            }
        }
    }

    /** Re-fetch a single order — used on screen resume and on a push wake-up. */
    fun loadDetail(id: Long) {
        viewModelScope.launch {
            (repo.detail(id) as? ApiResult.Success)?.let { r ->
                _state.update { state ->
                    state.copy(
                        selected = r.value,
                        orders = state.orders.map { if (it.id == id) r.value else it },
                    )
                }
            }
        }
    }

    fun cancel(id: Long) {
        viewModelScope.launch {
            when (val r = repo.cancel(id)) {
                is ApiResult.Success -> _state.update { state ->
                    state.copy(
                        selected = r.value,
                        orders = state.orders.map { if (it.id == id) r.value else it },
                    )
                }
                is ApiResult.HttpError -> _state.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _state.update { it.copy(error = r.cause.message) }
            }
        }
    }
}
