package com.ostarosto.app.domain.model

data class Customer(
    val id: Long,
    val name: String,
    val dialCode: Int,
    val phone: String,
    val email: String?,
    val loyaltyPoints: Double,
    val needsProfile: Boolean,
)

data class Branch(
    val id: Long,
    val foodicsId: String?,
    val name: String,
    val address: String?,
    val phone: String?,
    val latitude: Double?,
    val longitude: Double?,
    val isOpen: Boolean,
    val acceptsPickup: Boolean,
    val acceptsDelivery: Boolean,
    val hoursToday: BranchHours?,
)

data class BranchHours(val from: String?, val to: String?)

data class Category(
    val id: Long,
    val foodicsId: String?,
    val name: String,
    val image: String?,
    val sortOrder: Int,
)

data class NutritionFacts(
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fats: Double = 0.0,
)

data class Product(
    val id: Long,
    val foodicsId: String?,
    val name: String,
    val description: String?,
    val image: String?,
    val price: Double,
    val hasDiscount: Boolean,
    val originalPrice: Double?,
    val isPickupAvailable: Boolean,
    val isDeliveryAvailable: Boolean,
    val isOutOfStock: Boolean,
    val categoryId: Long?,
    val nutrition: NutritionFacts,
    val modifiers: List<Modifier> = emptyList(),
    /** True if the product has modifiers to configure, even on endpoints that omit the full list. */
    val hasModifiers: Boolean = modifiers.isNotEmpty(),
)

data class Modifier(
    val id: Long,
    val foodicsId: String?,
    val name: String,
    val selectionType: String,
    val minSelection: Int,
    val maxSelection: Int,
    val options: List<ModifierOption>,
)

data class ModifierOption(
    val id: Long,
    val foodicsId: String?,
    val name: String,
    val price: Double,
    val image: String?,
    val isOutOfStock: Boolean,
)

data class Combo(
    val id: Long,
    val foodicsId: String?,
    val comboSizeId: String?,
    val name: String,
    val description: String?,
    val image: String?,
    val price: Double,
    val hasDiscount: Boolean,
    val originalPrice: Double?,
    val nutrition: NutritionFacts,
)

data class HeroSlide(
    val id: Long,
    val title: String?,
    val description: String?,
    val badgeText: String?,
    val imageUrl: String?,
    val videoUrl: String?,
    val orderOnlineLink: String?,
    val sortOrder: Int,
)

data class PromotionalBanner(
    val id: Long,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val backgroundColor: String?,
    val textColor: String?,
    val discountText: String?,
    val dealText: String?,
    val linkUrl: String?,
    val sortOrder: Int,
)

data class PaymentMethod(
    val id: String,
    val name: String,
    val code: String?,
    val isPaymob: Boolean,
    val isWalletEnabled: Boolean = false,
)

/** Which Paymob channel to route a Paymob payment method through. */
enum class PaymentChannel(val apiValue: String) {
    Card("card"),
    Wallet("wallet"),
}
