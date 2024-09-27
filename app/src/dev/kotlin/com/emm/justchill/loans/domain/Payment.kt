package com.emm.justchill.loans.domain

data class Payment(
    val paymentId: String,
    val loanId: String,
    val dueDate: Long,
    val amount: Double,
    val status: String,
)