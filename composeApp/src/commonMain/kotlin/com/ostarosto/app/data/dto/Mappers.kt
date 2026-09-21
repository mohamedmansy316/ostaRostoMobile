package com.ostarosto.app.data.dto

import com.ostarosto.app.domain.model.Branch
import com.ostarosto.app.domain.model.BranchHours
import com.ostarosto.app.domain.model.Category
import com.ostarosto.app.domain.model.Combo
import com.ostarosto.app.domain.model.Customer
import com.ostarosto.app.domain.model.DeliveryAvailability
import com.ostarosto.app.domain.model.HeroSlide
import com.ostarosto.app.domain.model.Modifier
import com.ostarosto.app.domain.model.ModifierOption
import com.ostarosto.app.domain.model.NutritionFacts
import com.ostarosto.app.domain.model.Order
import com.ostarosto.app.domain.model.OrderBranchRef
import com.ostarosto.app.domain.model.OrderItem
import com.ostarosto.app.domain.model.OrderProgress
import com.ostarosto.app.domain.model.PaymentMethod
import com.ostarosto.app.domain.model.Product
import com.ostarosto.app.domain.model.PromotionalBanner
import com.ostarosto.app.domain.model.CartConflict
import com.ostarosto.app.domain.model.CartTotals
import com.ostarosto.app.domain.model.CartValidation

fun CustomerDto.toDomain() = Customer(
    id = id,
    name = name,
    dialCode = dialCode,
    phone = phone,
    email = email,
    loyaltyPoints = loyaltyPoints,
    needsProfile = needsProfile,
)

fun BranchDto.toDomain() = Branch(
    id = id,
    foodicsId = foodicsId,
    name = name,
    address = address,
    phone = phone,
    latitude = latitude,
    longitude = longitude,
    isOpen = isOpen,
    acceptsPickup = acceptsPickup,
    acceptsDelivery = acceptsDelivery,
    hoursToday = hoursToday?.let { BranchHours(it.from, it.to) },
)

fun CategoryDto.toDomain() = Category(id, foodicsId, name, image, sortOrder)

fun NutritionFactsDto.toDomain() = NutritionFacts(calories, protein, carbs, fats)

fun ModifierOptionDto.toDomain() = ModifierOption(id, foodicsId, name, price, image, isOutOfStock)

fun ModifierDto.toDomain() = Modifier(
    id = id,
    foodicsId = foodicsId,
    name = name,
    selectionType = selectionType,
    minSelection = minSelection,
    maxSelection = maxSelection,
    options = options.map { it.toDomain() },
)

fun ProductDto.toDomain() = Product(
    id = id,
    foodicsId = foodicsId,
    name = name,
    description = description,
    image = image,
    price = price,
    hasDiscount = hasDiscount,
    originalPrice = originalPrice,
    isPickupAvailable = isPickupAvailable,
    isDeliveryAvailable = isDeliveryAvailable,
    isOutOfStock = isOutOfStock,
    categoryId = categoryId,
    nutrition = nutrition.toDomain(),
    modifiers = modifiers.map { it.toDomain() },
    hasModifiers = hasModifiers || modifiers.isNotEmpty(),
)

fun ComboDto.toDomain() = Combo(
    id = id,
    foodicsId = foodicsId,
    comboSizeId = comboSizeId,
    name = name,
    description = description,
    image = image,
    price = price,
    hasDiscount = hasDiscount,
    originalPrice = originalPrice,
    nutrition = nutrition.toDomain(),
)

fun HeroSlideDto.toDomain() = HeroSlide(
    id = id,
    title = title,
    description = description,
    badgeText = badgeText,
    imageUrl = imageUrl,
    videoUrl = videoUrl,
    orderOnlineLink = orderOnlineLink,
    sortOrder = sortOrder,
)

fun PromotionalBannerDto.toDomain() = PromotionalBanner(
    id = id,
    title = title,
    subtitle = subtitle,
    imageUrl = imageUrl,
    backgroundColor = backgroundColor,
    textColor = textColor,
    discountText = discountText,
    dealText = dealText,
    linkUrl = linkUrl,
    sortOrder = sortOrder,
)

fun PaymentMethodDto.toDomain() = PaymentMethod(id, name, code, isPaymob, isWalletEnabled)

fun DeliveryCheckDto.toDomain() = DeliveryAvailability(
    available = available,
    zoneId = zoneId,
    zoneName = zoneName,
    branchId = branchId,
    branchName = branchName,
    deliveryFee = deliveryFee,
    distanceKm = distanceKm,
    estimatedTime = estimatedTime,
)

fun CartTotalsDto.toDomain() = CartTotals(subtotal, discount, tax, deliveryFee, total, currency, couponApplied)

fun CartConflictDto.toDomain() = CartConflict(id, cartItemId, type, issue, message, oldPrice, newPrice)

fun CartValidationDto.toDomain() = CartValidation(valid, conflicts.map { it.toDomain() })

fun OrderDto.toDomain() = Order(
    id = id,
    orderNumber = orderNumber,
    reference = reference,
    status = status,
    displayStatus = displayStatus,
    progress = OrderProgress.fromStep(progressStep),
    isPickup = isPickup,
    branch = branch?.let { OrderBranchRef(it.id, it.name, it.phone) },
    subtotal = subtotal,
    discount = discount,
    tax = tax,
    deliveryFee = deliveryFee,
    total = total,
    currency = currency,
    paymentMethod = paymentMethod,
    address = address,
    customerNotes = customerNotes,
    estimatedArrival = estimatedArrival,
    createdAt = createdAt,
    items = items.map {
        OrderItem(
            id = it.id,
            isCombo = it.isCombo,
            name = it.name,
            nameAr = it.nameAr,
            image = it.image,
            quantity = it.quantity,
            unitPrice = it.unitPrice,
            lineTotal = it.lineTotal,
            modifiers = it.modifiers.map { m -> m.nameAr.ifBlank { m.name } },
        )
    },
)
