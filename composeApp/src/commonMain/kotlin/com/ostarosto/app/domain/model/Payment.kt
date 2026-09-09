package com.ostarosto.app.domain.model

/** Outcome of POST /orders. */
sealed interface PlaceOrderOutcome {
    data class Placed(val order: Order) : PlaceOrderOutcome

    /** Card order — open [paymentUrl], then poll GET /payments/[reference]. */
    data class PaymentRequired(val paymentUrl: String, val reference: String) : PlaceOrderOutcome
}

enum class PaymentPoll { Pending, Paid, Failed }

data class PaymentResult(val poll: PaymentPoll, val order: Order?)
