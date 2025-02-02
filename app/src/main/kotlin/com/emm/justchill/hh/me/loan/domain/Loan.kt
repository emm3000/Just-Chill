package com.emm.justchill.hh.me.loan.domain

data class Loan(
    val loanId: String,
    val amount: Double,
    val amountWithInterest: Double,
    val interest: Long,
    val startDate: Long,
    val duration: Long,
    val status: String,
    val driverId: Long,
)