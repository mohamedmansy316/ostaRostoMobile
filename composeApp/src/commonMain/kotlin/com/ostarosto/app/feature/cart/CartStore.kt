package com.ostarosto.app.feature.cart

import com.ostarosto.app.domain.model.Cart
import com.ostarosto.app.domain.model.CartLine
import com.ostarosto.app.domain.model.Combo
import com.ostarosto.app.domain.model.Product
import com.ostarosto.app.domain.model.SelectedOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory cart shared across screens. A single Koin instance; not persisted
 * (an abandoned cart is fine to lose for the MVP).
 */
class CartStore {

    private val _cart = MutableStateFlow(Cart())
    val cart: StateFlow<Cart> = _cart.asStateFlow()

    fun addProduct(product: Product, quantity: Int, options: List<SelectedOption>, notes: String?) {
        val unit = product.price + options.sumOf { it.price * it.quantity }
        _cart.value = Cart(
            _cart.value.lines + CartLine(
                lineId = newLineId(),
                product = product,
                combo = null,
                quantity = quantity,
                unitPrice = unit,
                selectedOptions = options,
                notes = notes,
            ),
        )
    }

    fun addCombo(combo: Combo, quantity: Int) {
        _cart.value = Cart(
            _cart.value.lines + CartLine(
                lineId = newLineId(),
                product = null,
                combo = combo,
                quantity = quantity,
                unitPrice = combo.price,
            ),
        )
    }

    fun setQuantity(lineId: String, quantity: Int) {
        _cart.value = Cart(
            _cart.value.lines.mapNotNull { line ->
                when {
                    line.lineId != lineId -> line
                    quantity <= 0 -> null
                    else -> line.copy(quantity = quantity)
                }
            },
        )
    }

    fun remove(lineId: String) = setQuantity(lineId, 0)

    fun clear() {
        _cart.value = Cart()
    }

    private var counter = 0
    private fun newLineId(): String = "line-${counter++}-${(0..9999).random()}"
}
