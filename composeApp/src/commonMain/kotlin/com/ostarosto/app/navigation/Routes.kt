package com.ostarosto.app.navigation

/**
 * String routes for the Navigation-Compose graph. Kept simple (string-based)
 * so the FCM deep link `ostarosto://order/{id}` maps straight onto
 * [Route.OrderDetail].
 */
object Route {
    const val Splash = "splash"
    const val Auth = "auth"

    const val Menu = "menu"
    const val Profile = "profile"
    const val ProductDetail = "product/{productRef}"
    const val Cart = "cart"
    const val Checkout = "checkout"
    const val PaymentWaiting = "payment/{reference}"
    const val OrderConfirmation = "order-confirmation/{orderId}"
    const val Orders = "orders"
    const val OrderDetail = "order/{orderId}"

    fun productDetail(ref: String) = "product/$ref"
    fun paymentWaiting(reference: String) = "payment/$reference"
    fun orderConfirmation(orderId: Long) = "order-confirmation/$orderId"
    fun orderDetail(orderId: Long) = "order/$orderId"
}
