package com.ostarosto.app.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RequestOtpBody(
    @SerialName("dial_code") val dialCode: Int,
    val phone: String,
)

@Serializable
data class VerifyOtpBody(
    @SerialName("dial_code") val dialCode: Int,
    val phone: String,
    val otp: String,
    @SerialName("device_name") val deviceName: String? = null,
)

@Serializable
data class CompleteProfileBody(
    val name: String,
    val email: String? = null,
)

@Serializable
data class CartLineBody(
    @SerialName("product_id") val productId: String? = null,
    @SerialName("combo_id") val comboId: String? = null,
    val quantity: Int,
    @SerialName("unit_price") val unitPrice: Double? = null,
    // The web checkout reads standalone-product modifiers from `options`.
    val options: List<OrderModifierBody> = emptyList(),
    val notes: String? = null,
)

@Serializable
data class OrderModifierBody(
    @SerialName("modifier_option_id") val modifierOptionId: String,
    val quantity: Int = 1,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
)

@Serializable
data class CartTotalsBody(
    @SerialName("branch_id") val branchId: String,
    val type: Int,
    val products: List<CartLineBody> = emptyList(),
    val combos: List<CartLineBody> = emptyList(),
    @SerialName("coupon_code") val couponCode: String? = null,
    @SerialName("delivery_fee") val deliveryFee: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class ValidateCartItemBody(
    val id: String,
    val type: String,
    val quantity: Int,
    val price: Double,
    @SerialName("cart_item_id") val cartItemId: String? = null,
)

@Serializable
data class ValidateCartBody(
    @SerialName("branch_id") val branchId: String,
    @SerialName("order_type") val orderType: String,
    val items: List<ValidateCartItemBody>,
)

@Serializable
data class PlaceOrderBody(
    val type: Int,
    @SerialName("branch_id") val branchId: String,
    @SerialName("payment_method_id") val paymentMethodId: String,
    val products: List<CartLineBody> = emptyList(),
    val combos: List<CartLineBody> = emptyList(),
    @SerialName("coupon_code") val couponCode: String? = null,
    @SerialName("customer_notes") val customerNotes: String? = null,
    @SerialName("due_at") val dueAt: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("zone_id") val zoneId: Int? = null,
    @SerialName("delivery_fee") val deliveryFee: Double? = null,
)

@Serializable
data class RegisterDeviceBody(
    val token: String,
    val platform: String,
    @SerialName("app_version") val appVersion: String? = null,
)

@Serializable
data class TokenBody(val token: String)
