package com.ostarosto.app.feature.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ostarosto.app.core.network.ApiResult
import com.ostarosto.app.core.state.SelectionStore
import com.ostarosto.app.data.dto.CartLineBody
import com.ostarosto.app.data.dto.CartTotalsBody
import com.ostarosto.app.data.dto.OrderModifierBody
import com.ostarosto.app.data.dto.PlaceOrderBody
import com.ostarosto.app.data.repository.CartRepository
import com.ostarosto.app.data.repository.OrderRepository
import com.ostarosto.app.domain.model.Cart
import com.ostarosto.app.domain.model.CartTotals
import com.ostarosto.app.domain.model.Order
import com.ostarosto.app.domain.model.OrderType
import com.ostarosto.app.domain.model.PaymentMethod
import com.ostarosto.app.domain.model.PlaceOrderOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val orderType: OrderType = OrderType.Pickup,
    val branchName: String = "",
    val address: String = "",
    val notes: String = "",
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val selectedPaymentId: String? = null,
    val totals: CartTotals? = null,
    val loadingTotals: Boolean = false,
    val placing: Boolean = false,
    val placedOrder: Order? = null,
    /** Set for card orders — the app opens this Paymob URL then waits on [paymentReference]. */
    val paymentUrl: String? = null,
    val paymentReference: String? = null,
    val error: String? = null,
) {
    val canPlace: Boolean
        get() = selectedPaymentId != null && !placing &&
            (orderType == OrderType.Pickup || address.isNotBlank())
}

class CartViewModel(
    private val store: CartStore,
    private val selection: SelectionStore,
    private val cartRepo: CartRepository,
    private val orderRepo: OrderRepository,
) : ViewModel() {

    val cart: StateFlow<Cart> = store.cart

    private val _checkout = MutableStateFlow(CheckoutUiState())
    val checkout: StateFlow<CheckoutUiState> = _checkout.asStateFlow()

    private val branchRef: String? get() = selection.branchRef

    fun setQuantity(lineId: String, qty: Int) = store.setQuantity(lineId, qty)
    fun remove(lineId: String) = store.remove(lineId)

    fun setOrderType(type: OrderType) {
        selection.setOrderType(type)
        _checkout.update { it.copy(orderType = type) }
        refreshTotals()
    }

    fun setAddress(value: String) = _checkout.update { it.copy(address = value) }
    fun setNotes(value: String) = _checkout.update { it.copy(notes = value) }
    fun selectPayment(id: String) = _checkout.update { it.copy(selectedPaymentId = id) }

    /** Called when the checkout screen opens. */
    fun prepareCheckout() {
        _checkout.update {
            it.copy(
                orderType = selection.orderType.value,
                branchName = selection.branch.value?.name.orEmpty(),
            )
        }
        val ref = branchRef ?: return
        viewModelScope.launch {
            (cartRepo.paymentMethods(ref) as? ApiResult.Success)?.let { r ->
                _checkout.update { it.copy(paymentMethods = r.value, selectedPaymentId = r.value.firstOrNull()?.id) }
            }
        }
        refreshTotals()
    }

    fun refreshTotals() {
        val ref = branchRef ?: return
        _checkout.update { it.copy(loadingTotals = true) }
        viewModelScope.launch {
            val body = CartTotalsBody(
                branchId = ref,
                type = _checkout.value.orderType.apiValue,
                products = productLines(),
                combos = comboLines(),
            )
            when (val r = cartRepo.totals(body)) {
                is ApiResult.Success -> _checkout.update { it.copy(loadingTotals = false, totals = r.value, error = null) }
                is ApiResult.HttpError -> _checkout.update { it.copy(loadingTotals = false, error = r.message) }
                is ApiResult.NetworkError -> _checkout.update { it.copy(loadingTotals = false, error = r.cause.message) }
            }
        }
    }

    fun placeOrder() {
        val s = _checkout.value
        val ref = branchRef ?: return
        val paymentId = s.selectedPaymentId ?: return
        _checkout.update { it.copy(placing = true, error = null) }
        viewModelScope.launch {
            val body = PlaceOrderBody(
                type = s.orderType.apiValue,
                branchId = ref,
                paymentMethodId = paymentId,
                products = productLines(),
                combos = comboLines(),
                customerNotes = s.notes.ifBlank { null },
                address = if (s.orderType == OrderType.Delivery) s.address.ifBlank { null } else null,
            )
            when (val r = orderRepo.place(body)) {
                is ApiResult.Success -> when (val outcome = r.value) {
                    is PlaceOrderOutcome.Placed -> {
                        store.clear()
                        _checkout.update { it.copy(placing = false, placedOrder = outcome.order) }
                    }
                    is PlaceOrderOutcome.PaymentRequired -> {
                        store.clear()
                        _checkout.update {
                            it.copy(
                                placing = false,
                                paymentUrl = outcome.paymentUrl,
                                paymentReference = outcome.reference,
                            )
                        }
                    }
                }
                is ApiResult.HttpError -> _checkout.update { it.copy(placing = false, error = r.message) }
                is ApiResult.NetworkError -> _checkout.update { it.copy(placing = false, error = r.cause.message) }
            }
        }
    }

    fun consumePlacedOrder() = _checkout.update { it.copy(placedOrder = null) }

    fun consumePaymentRedirect() = _checkout.update { it.copy(paymentUrl = null, paymentReference = null) }

    fun reportCheckoutError(message: String) = _checkout.update { it.copy(error = message) }

    private fun productLines(): List<CartLineBody> = store.cart.value.lines
        .filterNot { it.isCombo }
        .map { line ->
            CartLineBody(
                productId = line.refId,
                quantity = line.quantity,
                unitPrice = line.unitPrice,
                options = line.selectedOptions.map { OrderModifierBody(it.modifierOptionId, it.quantity, it.price) },
                notes = line.notes,
            )
        }

    private fun comboLines(): List<CartLineBody> = store.cart.value.lines
        .filter { it.isCombo }
        .map { line -> CartLineBody(comboId = line.refId, quantity = line.quantity, unitPrice = line.unitPrice) }
}
