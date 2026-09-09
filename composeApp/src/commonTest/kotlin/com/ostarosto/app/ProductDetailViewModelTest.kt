package com.ostarosto.app

import com.ostarosto.app.data.repository.CatalogRepository
import com.ostarosto.app.feature.cart.CartStore
import com.ostarosto.app.feature.productdetail.ProductDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductDetailViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val productJson = """
        {"success":true,"data":{
          "id":9,"foodics_id":"p-9","name":"ساندويتش","description":null,"image":null,
          "price":100.0,"has_discount":false,"original_price":null,
          "is_pickup_available":true,"is_delivery_available":true,"is_out_of_stock":false,
          "category_id":1,"nutrition_facts":{"calories":0,"protein":0,"carbs":0,"fats":0},
          "modifiers":[
            {"id":1,"foodics_id":"m-1","name":"الحجم","selection_type":"single","min_selection":1,"max_selection":1,
             "options":[
               {"id":10,"foodics_id":"o-small","name":"صغير","price":0.0,"image":null,"is_out_of_stock":false},
               {"id":11,"foodics_id":"o-large","name":"كبير","price":25.0,"image":null,"is_out_of_stock":false}
             ]},
            {"id":2,"foodics_id":"m-2","name":"إضافات","selection_type":"multiple","min_selection":0,"max_selection":2,
             "options":[
               {"id":20,"foodics_id":"o-cheese","name":"جبنة","price":10.0,"image":null,"is_out_of_stock":false}
             ]}
          ]
        }}
    """.trimIndent()

    @Test
    fun required_multi_option_modifier_is_not_preselected_and_blocks_add() = runTest {
        val scope = testApi(ok(pathIs("/products/9"), productJson))
        val store = CartStore()
        val vm = ProductDetailViewModel(CatalogRepository(scope.api), store)

        vm.load("9", branchRef = null)
        val product = vm.state.value.product!!

        assertFalse(vm.state.value.requiredSatisfied(product), "required size group must be answered")
        assertTrue(product.modifiers.first().id in vm.state.value.unsatisfied(product))

        vm.addToCart()
        assertTrue(store.cart.value.isEmpty, "add is blocked while required options are missing")
        assertTrue(vm.state.value.showErrors)

        val large = product.modifiers.first().options[1]
        vm.toggleOption(product.modifiers.first(), large)
        assertTrue(vm.state.value.requiredSatisfied(product))
        assertFalse(vm.state.value.showErrors, "picking an option clears the error")
        assertEquals(125.0, vm.state.value.lineUnitPrice(product))
    }

    @Test
    fun multi_select_respects_max_selection() = runTest {
        val scope = testApi(ok(pathIs("/products/9"), productJson))
        val vm = ProductDetailViewModel(CatalogRepository(scope.api), CartStore())
        vm.load("9", null)
        val product = vm.state.value.product!!
        val addons = product.modifiers[1]

        vm.toggleOption(addons, addons.options[0]) // select cheese
        assertTrue(vm.state.value.selections[addons.id]!!.contains("o-cheese"))
        vm.toggleOption(addons, addons.options[0]) // deselect
        assertFalse(vm.state.value.selections[addons.id]!!.contains("o-cheese"))
    }

    @Test
    fun add_to_cart_pushes_a_configured_line_into_the_store() = runTest {
        val scope = testApi(ok(pathIs("/products/9"), productJson))
        val store = CartStore()
        val vm = ProductDetailViewModel(CatalogRepository(scope.api), store)
        vm.load("9", null)
        val product = vm.state.value.product!!
        vm.toggleOption(product.modifiers.first(), product.modifiers.first().options[1]) // large +25
        vm.setQuantity(2)

        vm.addToCart()

        val line = store.cart.value.lines.single()
        assertEquals(2, line.quantity)
        assertEquals(125.0, line.unitPrice)
        assertTrue(vm.state.value.added)
    }
}
