package com.ostarosto.app.domain.model

/** A configured line in the local cart (before it becomes an order payload). */
data class CartLine(
    val lineId: String,
    val product: Product?,
    val combo: Combo?,
    val quantity: Int,
    val unitPrice: Double,
    val selectedOptions: List<SelectedOption> = emptyList(),
    val notes: String? = null,
) {
    val isCombo: Boolean get() = combo != null
    val refId: String get() = (product?.foodicsId ?: combo?.foodicsId) ?: (product?.id ?: combo?.id).toString()
    val name: String get() = product?.name ?: combo?.name ?: ""
    val image: String? get() = product?.image ?: combo?.image
    val lineTotal: Double get() = unitPrice * quantity
}

data class SelectedOption(
    val modifierOptionId: String,
    val name: String,
    val price: Double,
    val quantity: Int = 1,
)

data class Cart(
    val lines: List<CartLine> = emptyList(),
) {
    val isEmpty: Boolean get() = lines.isEmpty()
    val itemCount: Int get() = lines.sumOf { it.quantity }
    val subtotal: Double get() = lines.sumOf { it.lineTotal }
}

data class CartTotals(
    val subtotal: Double,
    val discount: Double,
    val tax: Double,
    val deliveryFee: Double,
    val total: Double,
    val currency: String,
    val couponApplied: Boolean,
)

data class CartConflict(
    val id: String?,
    val cartItemId: String?,
    val type: String,
    val issue: String,
    val message: String,
    val oldPrice: Double? = null,
    val newPrice: Double? = null,
)

data class CartValidation(
    val valid: Boolean,
    val conflicts: List<CartConflict>,
)

enum class OrderType(val apiValue: Int) {
    Pickup(2),
    Delivery(3),
}
