package com.ostarosto.app.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerDto(
    val id: Long,
    val name: String = "",
    @SerialName("dial_code") val dialCode: Int = 20,
    val phone: String = "",
    val email: String? = null,
    @SerialName("loyalty_points") val loyaltyPoints: Double = 0.0,
    @SerialName("needs_profile") val needsProfile: Boolean = false,
)

@Serializable
data class VerifyOtpDataDto(
    val token: String,
    val customer: CustomerDto,
    @SerialName("needs_profile") val needsProfile: Boolean = false,
)

@Serializable
data class BranchHoursDto(val from: String? = null, val to: String? = null)

@Serializable
data class BranchDto(
    val id: Long,
    @SerialName("foodics_id") val foodicsId: String? = null,
    val name: String = "",
    val address: String? = null,
    val phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("is_open") val isOpen: Boolean = true,
    @SerialName("accepts_pickup") val acceptsPickup: Boolean = true,
    @SerialName("accepts_delivery") val acceptsDelivery: Boolean = true,
    @SerialName("hours_today") val hoursToday: BranchHoursDto? = null,
)

@Serializable
data class CategoryDto(
    val id: Long,
    @SerialName("foodics_id") val foodicsId: String? = null,
    val name: String = "",
    val image: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class NutritionFactsDto(
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fats: Double = 0.0,
)

@Serializable
data class ModifierOptionDto(
    val id: Long,
    @SerialName("foodics_id") val foodicsId: String? = null,
    val name: String = "",
    val price: Double = 0.0,
    val image: String? = null,
    @SerialName("is_out_of_stock") val isOutOfStock: Boolean = false,
)

@Serializable
data class ModifierDto(
    val id: Long,
    @SerialName("foodics_id") val foodicsId: String? = null,
    val name: String = "",
    @SerialName("selection_type") val selectionType: String = "single",
    @SerialName("min_selection") val minSelection: Int = 0,
    @SerialName("max_selection") val maxSelection: Int = 1,
    val options: List<ModifierOptionDto> = emptyList(),
)

@Serializable
data class ProductDto(
    val id: Long,
    @SerialName("foodics_id") val foodicsId: String? = null,
    val name: String = "",
    val description: String? = null,
    val image: String? = null,
    val price: Double = 0.0,
    @SerialName("has_discount") val hasDiscount: Boolean = false,
    @SerialName("original_price") val originalPrice: Double? = null,
    @SerialName("is_pickup_available") val isPickupAvailable: Boolean = true,
    @SerialName("is_delivery_available") val isDeliveryAvailable: Boolean = true,
    @SerialName("is_out_of_stock") val isOutOfStock: Boolean = false,
    @SerialName("category_id") val categoryId: Long? = null,
    @SerialName("nutrition_facts") val nutrition: NutritionFactsDto = NutritionFactsDto(),
    val modifiers: List<ModifierDto> = emptyList(),
)

@Serializable
data class ComboDto(
    val id: Long,
    @SerialName("foodics_id") val foodicsId: String? = null,
    @SerialName("combo_size_id") val comboSizeId: String? = null,
    val name: String = "",
    val description: String? = null,
    val image: String? = null,
    val price: Double = 0.0,
    @SerialName("has_discount") val hasDiscount: Boolean = false,
    @SerialName("original_price") val originalPrice: Double? = null,
    @SerialName("nutrition_facts") val nutrition: NutritionFactsDto = NutritionFactsDto(),
)

@Serializable
data class PaymentMethodDto(
    val id: String,
    val name: String = "",
    val code: String? = null,
    @SerialName("is_paymob") val isPaymob: Boolean = false,
)

@Serializable
data class CartTotalsDto(
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    @SerialName("delivery_fee") val deliveryFee: Double = 0.0,
    val total: Double = 0.0,
    val currency: String = "EGP",
    @SerialName("coupon_applied") val couponApplied: Boolean = false,
)

@Serializable
data class CartConflictDto(
    val id: String? = null,
    @SerialName("cart_item_id") val cartItemId: String? = null,
    val type: String = "",
    val issue: String = "",
    val message: String = "",
    @SerialName("old_price") val oldPrice: Double? = null,
    @SerialName("new_price") val newPrice: Double? = null,
)

@Serializable
data class CartValidationDto(
    val valid: Boolean = true,
    val conflicts: List<CartConflictDto> = emptyList(),
)

@Serializable
data class OrderBranchDto(
    val id: Long? = null,
    val name: String? = null,
    val phone: String? = null,
)

@Serializable
data class OrderItemModifierDto(val name: String = "", @SerialName("name_ar") val nameAr: String = "")

@Serializable
data class OrderItemDto(
    val id: String? = null,
    @SerialName("is_combo") val isCombo: Boolean = false,
    val name: String = "",
    @SerialName("name_ar") val nameAr: String = "",
    val image: String? = null,
    val quantity: Int = 1,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    @SerialName("line_total") val lineTotal: Double = 0.0,
    val modifiers: List<OrderItemModifierDto> = emptyList(),
)

@Serializable
data class PlaceOrderResponseDto(
    @SerialName("requires_payment") val requiresPayment: Boolean = false,
    @SerialName("payment_url") val paymentUrl: String? = null,
    val reference: String? = null,
    val order: OrderDto? = null,
)

@Serializable
data class PaymentStatusDto(
    val status: String = "pending",
    val reference: String = "",
    val order: OrderDto? = null,
)

@Serializable
data class OrderDto(
    val id: Long,
    @SerialName("order_number") val orderNumber: String = "",
    val reference: String? = null,
    val status: String = "",
    @SerialName("display_status") val displayStatus: String = "",
    @SerialName("progress_step") val progressStep: Int = 1,
    @SerialName("is_pickup") val isPickup: Boolean = true,
    val branch: OrderBranchDto? = null,
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    @SerialName("delivery_fee") val deliveryFee: Double = 0.0,
    val total: Double = 0.0,
    val currency: String = "EGP",
    @SerialName("payment_method") val paymentMethod: String? = null,
    val address: String? = null,
    @SerialName("customer_notes") val customerNotes: String? = null,
    @SerialName("estimated_arrival") val estimatedArrival: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val items: List<OrderItemDto> = emptyList(),
)
