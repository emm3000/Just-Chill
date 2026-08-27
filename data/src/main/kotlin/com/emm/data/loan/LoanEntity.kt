package com.emm.data.loan

data class LoanEntity(
    val loanId: String,
    val personName: String,
    val personKey: String,
    val principal: Long,
    val interestBps: Long,
    val totalDue: Long,
    val note: String,
    val lentAt: String,
    val createdAt: Long,
    val updatedAt: Long,
)
