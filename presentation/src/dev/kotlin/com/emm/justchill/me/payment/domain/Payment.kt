package com.emm.justchill.me.payment.domain

data class Payment(
    val paymentId: String,
    val loanId: String,
    val dueDate: Long,
    val amount: Double,
    val status: PaymentStatus,
)