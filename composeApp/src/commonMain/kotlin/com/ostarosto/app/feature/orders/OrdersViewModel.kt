package com.ostarosto.app.feature.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ostarosto.app.core.network.ApiResult
import com.ostarosto.app.data.repository.CatalogRepository
import com.ostarosto.app.data.repository.OrderRepository
import com.ostarosto.app.domain.model.Order
import com.ostarosto.app.feature.cart.CartStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Outcome of a reorder pass: how many of the order's lines actually made it into the cart. */
data class ReorderResult(val added: Int, val skipped: Int)

data class OrdersUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val orders: List<Order> = emptyList(),
    val selected: Order? = null,
    val error: String? = null,
    val reorderingId: Long? = null,
    val reorderResult: ReorderResult? = null,
)

class OrdersViewModel(
    private val repo: OrderRepository,
    private val catalog: CatalogRepository,
    private val cart: CartStore,
) : ViewModel() {

    private val _state = MutableStateFlow(OrdersUiState())
    val state: StateFlow<OrdersUiState> = _state.asStateFlow()

    /** @param pull true when triggered by pull-to-refresh (keep the list visible). */
    fun refresh(pull: Boolean = false) {
        if (_state.value.refreshing) return
        _state.update { if (pull) it.copy(refreshing = true, error = null) else it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.list()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, refreshing = false, orders = r.value) }
                is ApiResult.HttpError -> _state.update { it.copy(loading = false, refreshing = false, error = r.message) }
                is ApiResult.NetworkError -> _state.update { it.copy(loading = false, refreshing = false, error = r.cause.message) }
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

    /**
     * Re-adds an order's lines to the cart. Each line is re-fetched from the
     * catalogue by its ref id so pricing, modifiers and stock reflect today —
     * lines that no longer exist or are out of stock are silently skipped.
     */
    fun reorder(order: Order) {
        if (_state.value.reorderingId != null) return
        _state.update { it.copy(reorderingId = order.id) }
        viewModelScope.launch {
            val branchId = order.branch?.id?.toString()
            var added = 0
            var skipped = 0
            for (item in order.items) {
                val ref = item.id
                if (ref == null) {
                    skipped++
                    continue
                }
                if (item.isCombo) {
                    when (val r = catalog.combo(ref, branchId)) {
                        is ApiResult.Success -> {
                            cart.addCombo(r.value, item.quantity)
                            added++
                        }
                        else -> skipped++
                    }
                } else {
                    when (val r = catalog.product(ref, branchId)) {
                        is ApiResult.Success -> if (r.value.isOutOfStock) {
                            skipped++
                        } else {
                            cart.addProduct(r.value, item.quantity, emptyList(), null)
                            added++
                        }
                        else -> skipped++
                    }
                }
            }
            _state.update { it.copy(reorderingId = null, reorderResult = ReorderResult(added, skipped)) }
        }
    }

    fun consumeReorderResult() = _state.update { it.copy(reorderResult = null) }

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
