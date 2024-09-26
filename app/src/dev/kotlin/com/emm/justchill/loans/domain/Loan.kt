package com.emm.justchill.loans.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Loan(

    @SerialName("loan_id")
    val loanId: String,

    val amount: Double,

    @SerialName("amount_with_interest")
    val amountWithInterest: Double,

    val interest: Long,

    @SerialName("start_date")
    val startDate: Long,

    val duration: Long,

    @SerialName("payment_frequency")
    val paymentFrequency: String,

    val status: String,
)