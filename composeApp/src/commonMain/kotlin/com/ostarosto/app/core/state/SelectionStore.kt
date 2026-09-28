package com.ostarosto.app.core.state

import com.ostarosto.app.domain.model.Branch
import com.ostarosto.app.domain.model.OrderType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The customer's confirmed delivery destination — set once a map pin or a
 * saved address has been checked against the branch's zones. Delivery zones
 * are polygons server-side (no single "center" point), so this always carries
 * a real point plus whatever human-readable text we have for it, preferring
 * a saved address's label/details over the abstract zone name.
 */
data class DeliveryDestination(
    val latitude: Double,
    val longitude: Double,
    val zoneId: Int?,
    val zoneName: String?,
    val deliveryFee: Double,
    val addressLabel: String? = null,
    val addressText: String? = null,
) {
    val displayText: String
        get() = addressLabel
            ?: addressText?.takeIf { it.isNotBlank() }
            ?: zoneName.orEmpty()
}

/**
 * App-wide selections that outlive a single screen: which branch the customer
 * is ordering from, whether it is pickup or delivery, and — for delivery —
 * the confirmed destination, so screens other than Checkout (e.g. the branch
 * picker) can also show where the order is headed. A single Koin instance.
 */
class SelectionStore {

    private val _branch = MutableStateFlow<Branch?>(null)
    val branch: StateFlow<Branch?> = _branch.asStateFlow()

    private val _orderType = MutableStateFlow(OrderType.Pickup)
    val orderType: StateFlow<OrderType> = _orderType.asStateFlow()

    private val _deliveryDestination = MutableStateFlow<DeliveryDestination?>(null)
    val deliveryDestination: StateFlow<DeliveryDestination?> = _deliveryDestination.asStateFlow()

    fun setBranch(branch: Branch) {
        _branch.value = branch
    }

    fun setOrderType(type: OrderType) {
        _orderType.value = type
        // A destination only makes sense for delivery — drop a stale one so a
        // later Pickup -> Delivery toggle doesn't silently resurrect it.
        if (type == OrderType.Pickup) _deliveryDestination.value = null
    }

    fun setDeliveryDestination(destination: DeliveryDestination) {
        _deliveryDestination.value = destination
    }

    fun clearDeliveryDestination() {
        _deliveryDestination.value = null
    }

    /** Foodics UUID when available, else the local id — the form both accept. */
    val branchRef: String?
        get() = _branch.value?.let { it.foodicsId ?: it.id.toString() }
}
