package com.ostarosto.app.domain.model

/**
 * Tracking step, mirrors Order::progress_step on the backend.
 * 0 cancelled/declined · 1 received · 2 preparing · 3 ready / on the way · 4 done.
 */
enum class OrderProgress(val step: Int) {
    Cancelled(0),
    Received(1),
    Preparing(2),
    Ready(3),
    Completed(4);

    companion object {
        fun fromStep(step: Int): OrderProgress =
            entries.firstOrNull { it.step == step } ?: Received
    }
}

data class OrderItem(
    val id: String?,
    val isCombo: Boolean,
    val name: String,
    val nameAr: String,
    val image: String?,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double,
    val modifiers: List<String>,
)

data class OrderBranchRef(
    val id: Long?,
    val name: String?,
    val phone: String?,
)

data class Order(
    val id: Long,
    val orderNumber: String,
    val reference: String?,
    val status: String,
    val displayStatus: String,
    val progress: OrderProgress,
    val isPickup: Boolean,
    val branch: OrderBranchRef?,
    val subtotal: Double,
    val discount: Double,
    val tax: Double,
    val deliveryFee: Double,
    val total: Double,
    val currency: String,
    val paymentMethod: String?,
    val address: String?,
    val customerNotes: String?,
    val estimatedArrival: String?,
    val createdAt: String?,
    val items: List<OrderItem> = emptyList(),
)
