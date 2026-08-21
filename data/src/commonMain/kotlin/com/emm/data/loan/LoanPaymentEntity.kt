package com.emm.data.loan

data class LoanPaymentEntity(
    val paymentId: String,
    val loanId: String,
    val amount: Long,
    val method: String,
    val paidAt: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)
