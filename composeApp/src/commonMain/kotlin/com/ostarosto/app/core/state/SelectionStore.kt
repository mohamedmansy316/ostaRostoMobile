package com.ostarosto.app.core.state

import com.ostarosto.app.domain.model.Branch
import com.ostarosto.app.domain.model.OrderType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide selections that outlive a single screen: which branch the customer
 * is ordering from and whether it is pickup or delivery. A single Koin instance.
 */
class SelectionStore {

    private val _branch = MutableStateFlow<Branch?>(null)
    val branch: StateFlow<Branch?> = _branch.asStateFlow()

    private val _orderType = MutableStateFlow(OrderType.Pickup)
    val orderType: StateFlow<OrderType> = _orderType.asStateFlow()

    fun setBranch(branch: Branch) {
        _branch.value = branch
    }

    fun setOrderType(type: OrderType) {
        _orderType.value = type
    }

    /** Foodics UUID when available, else the local id — the form both accept. */
    val branchRef: String?
        get() = _branch.value?.let { it.foodicsId ?: it.id.toString() }
}
