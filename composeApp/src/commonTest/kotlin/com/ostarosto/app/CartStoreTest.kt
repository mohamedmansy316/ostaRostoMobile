package com.ostarosto.app

import com.ostarosto.app.domain.model.NutritionFacts
import com.ostarosto.app.domain.model.Product
import com.ostarosto.app.domain.model.SelectedOption
import com.ostarosto.app.feature.cart.CartStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CartStoreTest {

    private fun product(price: Double) = Product(
        id = 1, foodicsId = "p-1", name = "برجر", description = null, image = null,
        price = price, hasDiscount = false, originalPrice = null,
        isPickupAvailable = true, isDeliveryAvailable = true, isOutOfStock = false,
        categoryId = null, nutrition = NutritionFacts(),
    )

    @Test
    fun adding_a_product_folds_option_prices_into_the_unit_price() {
        val store = CartStore()
        store.addProduct(
            product(100.0),
            quantity = 2,
            options = listOf(SelectedOption("o-1", "جبنة", 15.0), SelectedOption("o-2", "بيكون", 20.0)),
            notes = null,
        )

        val line = store.cart.value.lines.single()
        assertEquals(135.0, line.unitPrice)
        assertEquals(270.0, line.lineTotal)
        assertEquals(2, store.cart.value.itemCount)
        assertEquals(270.0, store.cart.value.subtotal)
    }

    @Test
    fun setting_quantity_to_zero_removes_the_line() {
        val store = CartStore()
        store.addProduct(product(50.0), 1, emptyList(), null)
        val lineId = store.cart.value.lines.single().lineId

        store.setQuantity(lineId, 0)

        assertTrue(store.cart.value.isEmpty)
    }

    @Test
    fun clear_empties_the_cart() {
        val store = CartStore()
        store.addProduct(product(50.0), 3, emptyList(), null)
        store.clear()
        assertTrue(store.cart.value.isEmpty)
    }
}
