package com.ostarosto.app.feature.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ostarosto.app.core.l10n.Ar
import com.ostarosto.app.core.network.ApiResult
import com.ostarosto.app.core.state.DeliveryDestination
import com.ostarosto.app.core.state.SelectionStore
import com.ostarosto.app.data.dto.CartLineBody
import com.ostarosto.app.data.dto.CartTotalsBody
import com.ostarosto.app.data.dto.OrderModifierBody
import com.ostarosto.app.data.dto.PlaceOrderBody
import com.ostarosto.app.data.repository.CartRepository
import com.ostarosto.app.data.repository.DeliveryRepository
import com.ostarosto.app.data.repository.OrderRepository
import com.ostarosto.app.domain.model.Cart
import com.ostarosto.app.domain.model.CartTotals
import com.ostarosto.app.domain.model.DeliveryAvailability
import com.ostarosto.app.domain.model.DeliveryZone
import com.ostarosto.app.domain.model.Order
import com.ostarosto.app.domain.model.OrderType
import com.ostarosto.app.domain.model.PaymentChannel
import com.ostarosto.app.domain.model.PaymentMethod
import com.ostarosto.app.domain.model.PlaceOrderOutcome
import com.ostarosto.app.domain.model.SavedAddress
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
    /** Set once a map pin is confirmed on the location picker (delivery orders only). */
    val latitude: Double? = null,
    val longitude: Double? = null,
    val zoneId: Int? = null,
    val deliveryZoneName: String? = null,
    val deliveryFee: Double? = null,
    val zones: List<DeliveryZone> = emptyList(),
    val loadingZones: Boolean = false,
    val selectedAddressId: String? = null,
    /** Saved address label ("Home"/"Work") when one is selected — shown instead of the zone name. */
    val selectedAddressLabel: String? = null,
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val selectedPaymentId: String? = null,
    val selectedChannel: PaymentChannel = PaymentChannel.Card,
    val totals: CartTotals? = null,
    val loadingTotals: Boolean = false,
    val placing: Boolean = false,
    val placedOrder: Order? = null,
    /** Set for card orders — the app opens this Paymob URL then waits on [paymentReference]. */
    val paymentUrl: String? = null,
    val paymentReference: String? = null,
    val error: String? = null,
) {
    /** What to show as the delivery destination summary — real address details over the abstract zone name. */
    val deliveryDisplayText: String?
        get() = selectedAddressLabel ?: address.takeIf { it.isNotBlank() } ?: deliveryZoneName

    val canPlace: Boolean
        get() = selectedPaymentId != null && !placing &&
            (orderType == OrderType.Pickup || (latitude != null && longitude != null && address.isNotBlank()))
}

class CartViewModel(
    private val store: CartStore,
    private val selection: SelectionStore,
    private val cartRepo: CartRepository,
    private val orderRepo: OrderRepository,
    private val deliveryRepo: DeliveryRepository,
) : ViewModel() {

    val cart: StateFlow<Cart> = store.cart

    private val _checkout = MutableStateFlow(CheckoutUiState())
    val checkout: StateFlow<CheckoutUiState> = _checkout.asStateFlow()

    private val branchRef: String? get() = selection.branchRef

    fun setQuantity(lineId: String, qty: Int) = store.setQuantity(lineId, qty)
    fun remove(lineId: String) = store.remove(lineId)

    fun setOrderType(type: OrderType) {
        // SelectionStore itself drops a stale delivery destination when switching
        // to Pickup — see SelectionStore.setOrderType.
        selection.setOrderType(type)
        _checkout.update {
            if (type == OrderType.Pickup) {
                it.copy(
                    orderType = type,
                    latitude = null,
                    longitude = null,
                    zoneId = null,
                    deliveryZoneName = null,
                    deliveryFee = null,
                    selectedAddressId = null,
                    selectedAddressLabel = null,
                )
            } else {
                it.copy(orderType = type)
            }
        }
        if (type == OrderType.Delivery) loadZones()
        refreshTotals()
    }

    fun setAddress(value: String) = _checkout.update { it.copy(address = value) }
    fun setNotes(value: String) = _checkout.update { it.copy(notes = value) }
    fun selectPayment(id: String, channel: PaymentChannel = PaymentChannel.Card) =
        _checkout.update { it.copy(selectedPaymentId = id, selectedChannel = channel) }

    /** Called when the location picker confirms an available map pin. */
    fun setLocation(latitude: Double, longitude: Double, availability: DeliveryAvailability) {
        _checkout.update {
            it.copy(
                latitude = latitude,
                longitude = longitude,
                zoneId = availability.zoneId,
                deliveryZoneName = availability.zoneName,
                deliveryFee = availability.deliveryFee,
                selectedAddressId = null,
                selectedAddressLabel = null,
            )
        }
        if (availability.available) {
            selection.setDeliveryDestination(
                DeliveryDestination(
                    latitude = latitude,
                    longitude = longitude,
                    zoneId = availability.zoneId,
                    zoneName = availability.zoneName,
                    deliveryFee = availability.deliveryFee,
                    addressText = _checkout.value.address.ifBlank { null },
                ),
            )
        }
        refreshTotals()
    }

    /** Called when the user taps a saved address chip — re-checks it against the zones. */
    fun selectSavedAddress(address: SavedAddress) {
        _checkout.update {
            it.copy(selectedAddressId = address.id, selectedAddressLabel = address.label, address = address.addressText.orEmpty())
        }
        val ref = branchRef ?: return
        viewModelScope.launch {
            when (val r = deliveryRepo.check(address.latitude, address.longitude, ref)) {
                is ApiResult.Success -> {
                    val availability = r.value
                    _checkout.update {
                        it.copy(
                            latitude = address.latitude,
                            longitude = address.longitude,
                            zoneId = availability.zoneId,
                            deliveryZoneName = availability.zoneName,
                            deliveryFee = availability.deliveryFee,
                            error = if (!availability.available) Ar.deliveryNotAvailableHere else null,
                        )
                    }
                    if (availability.available) {
                        selection.setDeliveryDestination(
                            DeliveryDestination(
                                latitude = address.latitude,
                                longitude = address.longitude,
                                zoneId = availability.zoneId,
                                zoneName = availability.zoneName,
                                deliveryFee = availability.deliveryFee,
                                addressLabel = address.label,
                                addressText = address.addressText,
                            ),
                        )
                    }
                    refreshTotals()
                }
                is ApiResult.HttpError -> _checkout.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _checkout.update { it.copy(error = r.cause.message) }
            }
        }
    }

    /** Browsable zone list for the branch — loaded once the delivery segment is shown. */
    fun loadZones() {
        val ref = branchRef ?: return
        _checkout.update { it.copy(loadingZones = true) }
        viewModelScope.launch {
            when (val r = deliveryRepo.listZones(ref)) {
                is ApiResult.Success -> _checkout.update { it.copy(loadingZones = false, zones = r.value) }
                else -> _checkout.update { it.copy(loadingZones = false) }
            }
        }
    }

    /** Called when the checkout screen opens. */
    fun prepareCheckout() {
        // A destination may already be confirmed (branch picker, or the app-launch
        // geofence prompt) before the user ever opens Checkout — seed it in rather
        // than starting from a blank map pin every time.
        val destination = selection.deliveryDestination.value
        _checkout.update {
            it.copy(
                orderType = selection.orderType.value,
                branchName = selection.branch.value?.name.orEmpty(),
                latitude = destination?.latitude ?: it.latitude,
                longitude = destination?.longitude ?: it.longitude,
                zoneId = destination?.zoneId ?: it.zoneId,
                deliveryZoneName = destination?.zoneName ?: it.deliveryZoneName,
                deliveryFee = destination?.deliveryFee ?: it.deliveryFee,
                selectedAddressLabel = destination?.addressLabel ?: it.selectedAddressLabel,
                address = destination?.addressText?.takeIf { text -> text.isNotBlank() } ?: it.address,
            )
        }
        val ref = branchRef ?: return
        viewModelScope.launch {
            (cartRepo.paymentMethods(ref) as? ApiResult.Success)?.let { r ->
                _checkout.update { it.copy(paymentMethods = r.value, selectedPaymentId = r.value.firstOrNull()?.id) }
            }
        }
        if (_checkout.value.orderType == OrderType.Delivery) loadZones()
        refreshTotals()
    }

    fun refreshTotals() {
        val ref = branchRef ?: return
        val s = _checkout.value
        _checkout.update { it.copy(loadingTotals = true) }
        viewModelScope.launch {
            val isDelivery = s.orderType == OrderType.Delivery
            val body = CartTotalsBody(
                branchId = ref,
                type = s.orderType.apiValue,
                products = productLines(),
                combos = comboLines(),
                deliveryFee = if (isDelivery) s.deliveryFee else null,
                latitude = if (isDelivery) s.latitude else null,
                longitude = if (isDelivery) s.longitude else null,
                zoneId = if (isDelivery) s.zoneId else null,
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
            val isDelivery = s.orderType == OrderType.Delivery
            val body = PlaceOrderBody(
                type = s.orderType.apiValue,
                branchId = ref,
                paymentMethodId = paymentId,
                paymentChannel = s.selectedChannel.apiValue,
                products = productLines(),
                combos = comboLines(),
                customerNotes = s.notes.ifBlank { null },
                address = if (isDelivery) s.address.ifBlank { null } else null,
                latitude = if (isDelivery) s.latitude else null,
                longitude = if (isDelivery) s.longitude else null,
                zoneId = if (isDelivery) s.zoneId else null,
                deliveryFee = if (isDelivery) s.deliveryFee else null,
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
