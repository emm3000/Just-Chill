package com.emm.justchill.loans.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentModel(

    @SerialName("payment_id")
    val paymentId: String,

    @SerialName("loan_id")
    val loanId: String,

    @SerialName("due_date")
    val dueDate: Long,
    val amount: Long,
    val status: String,

    @SerialName("payment_date")
    val paymentDate: Long? = null
)