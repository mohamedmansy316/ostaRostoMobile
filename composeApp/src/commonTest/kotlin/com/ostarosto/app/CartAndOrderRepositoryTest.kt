package com.ostarosto.app

import com.ostarosto.app.core.network.ApiResult
import com.ostarosto.app.core.network.unwrap
import com.ostarosto.app.data.dto.CartLineBody
import com.ostarosto.app.data.dto.CartTotalsBody
import com.ostarosto.app.data.dto.PlaceOrderBody
import com.ostarosto.app.data.dto.ValidateCartBody
import com.ostarosto.app.data.dto.ValidateCartItemBody
import com.ostarosto.app.data.repository.CartRepository
import com.ostarosto.app.data.repository.OrderRepository
import com.ostarosto.app.domain.model.OrderProgress
import com.ostarosto.app.domain.model.PlaceOrderOutcome
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CartAndOrderRepositoryTest {

    @Test
    fun totals_parse_the_full_breakdown() = runTest {
        val scope = testApi(
            ok(pathIs("/cart/totals"), """
                {"success":true,"data":{"subtotal":228.0,"discount":0.0,"tax":28.0,"delivery_fee":0.0,"total":228.0,"currency":"EGP","coupon_applied":false}}
            """.trimIndent()),
        )
        val repo = CartRepository(scope.api)

        val totals = repo.totals(
            CartTotalsBody(branchId = "b-1", type = 2, products = listOf(CartLineBody(productId = "p-1", quantity = 2))),
        ).unwrap()

        assertEquals(228.0, totals.total)
        assertEquals("EGP", totals.currency)
    }

    @Test
    fun validate_surfaces_a_branch_closed_conflict() = runTest {
        val scope = testApi(
            ok(pathIs("/cart/validate"), """
                {"success":true,"data":{"valid":false,"conflicts":[
                  {"id":null,"cart_item_id":null,"type":"branch","issue":"branch_closed","message":"الفرع مغلق"}
                ]}}
            """.trimIndent()),
        )
        val repo = CartRepository(scope.api)

        val validation = repo.validate(
            ValidateCartBody(
                branchId = "b-1",
                orderType = "pickup",
                items = listOf(ValidateCartItemBody(id = "p-1", type = "product", quantity = 1, price = 50.0)),
            ),
        ).unwrap()

        assertEquals(false, validation.valid)
        assertEquals("branch_closed", validation.conflicts.single().issue)
    }

    @Test
    fun place_order_returns_the_created_order() = runTest {
        val scope = testApi(
            created(pathIs("/orders"), """
                {"success":true,"message":"تم","data":{
                  "requires_payment":false,"payment_url":null,"reference":null,
                  "order":{
                    "id":51,"order_number":"ORD-2519876","reference":null,"status":"pending_approval",
                    "display_status":"تم الاستلام","progress_step":1,"is_pickup":true,"type":"2",
                    "branch":{"id":1,"name":"مدينة نصر","phone":"0100"},
                    "subtotal":120.0,"discount":0.0,"tax":14.74,"delivery_fee":0.0,"total":120.0,"currency":"EGP",
                    "payment_method":"Cash","address":null,"customer_notes":null,
                    "estimated_arrival":null,"created_at":"2026-09-08T20:00:00+00:00","items":[]
                  }
                }}
            """.trimIndent()),
        )
        val repo = OrderRepository(scope.api)

        val outcome = repo.place(
            PlaceOrderBody(
                type = 2,
                branchId = "b-1",
                paymentMethodId = "cash",
                products = listOf(CartLineBody(productId = "p-1", quantity = 1, unitPrice = 120.0)),
            ),
        ).unwrap()

        val placed = outcome as PlaceOrderOutcome.Placed
        assertEquals(51L, placed.order.id)
        assertEquals(OrderProgress.Received, placed.order.progress)
    }

    @Test
    fun place_order_with_a_card_method_returns_a_payment_redirect() = runTest {
        val scope = testApi(
            ok(pathIs("/orders"), """
                {"success":true,"message":"redirect","data":{
                  "requires_payment":true,
                  "payment_url":"https://accept.paymob.com/unifiedcheckout/?x=1",
                  "reference":"ORD-251-8905","order":null
                }}
            """.trimIndent()),
        )
        val repo = OrderRepository(scope.api)

        val outcome = repo.place(
            PlaceOrderBody(type = 2, branchId = "b-1", paymentMethodId = "card"),
        ).unwrap()

        val redirect = outcome as PlaceOrderOutcome.PaymentRequired
        assertEquals("ORD-251-8905", redirect.reference)
        assertTrue(redirect.paymentUrl.contains("paymob"))
    }

    @Test
    fun payment_status_maps_paid_pending_and_failed() = runTest {
        val scope = testApi(
            status(200, pathIs("/payments/ref-paid"), """
                {"success":true,"data":{"status":"paid","reference":"ref-paid","order":{"id":7,"order_number":"X","status":"approved","display_status":"","progress_step":2,"is_pickup":true,"total":50.0,"currency":"EGP"}}}
            """.trimIndent()),
            status(200, pathIs("/payments/ref-pending"), """{"success":true,"data":{"status":"pending","reference":"ref-pending","order":null}}"""),
            status(200, pathIs("/payments/ref-failed"), """{"success":true,"data":{"status":"failed","reference":"ref-failed","order":null}}"""),
        )
        val repo = com.ostarosto.app.data.repository.PaymentRepository(scope.api)

        val paid = repo.status("ref-paid").unwrap()
        assertEquals(com.ostarosto.app.domain.model.PaymentPoll.Paid, paid.poll)
        assertEquals(7L, paid.order?.id)

        assertEquals(com.ostarosto.app.domain.model.PaymentPoll.Pending, repo.status("ref-pending").unwrap().poll)
        assertEquals(com.ostarosto.app.domain.model.PaymentPoll.Failed, repo.status("ref-failed").unwrap().poll)
    }

    @Test
    fun list_orders_maps_progress_steps() = runTest {
        val scope = testApi(
            ok(pathIs("/orders"), """
                {"success":true,"data":[
                  {"id":1,"order_number":"A","status":"approved","display_status":"","progress_step":3,"is_pickup":false,"total":100.0,"currency":"EGP"},
                  {"id":2,"order_number":"B","status":"cancelled","display_status":"","progress_step":0,"is_pickup":true,"total":0.0,"currency":"EGP"}
                ]}
            """.trimIndent()),
        )
        val repo = OrderRepository(scope.api)

        val orders = repo.list().unwrap()

        assertEquals(OrderProgress.Ready, orders[0].progress)
        assertEquals(OrderProgress.Cancelled, orders[1].progress)
    }

    @Test
    fun a_network_failure_becomes_a_network_error() = runTest {
        val scope = testApi(
            ok(pathIs("/never-hit"), "{}"),
        )
        val repo = OrderRepository(scope.api)

        // No route matches -> MockEngine throws -> repository returns NetworkError.
        val result = repo.list()

        assertTrue(result is ApiResult.NetworkError)
    }
}
