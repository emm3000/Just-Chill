package com.emm.justchill.hh.me.payment.domain

data class Payment(
    val paymentId: String,
    val loanId: String,
    val dueDate: Long,
    val amount: Double,
    val status: PaymentStatus,
)