package com.emm.justchill.hh.transaction.data

import com.emm.justchill.Transactions
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionModel(

    @SerialName("transaction_id")
    val transactionId: String,

    val type: String,
    val amount: Double,
    val description: String,
    val date: Long,

    @SerialName("user_id")
    val userId: String = "",

    @SerialName("category_id")
    val categoryId: String?,

    @SerialName("account_id")
    val accountId: String,
)

fun Transactions.toModel() = TransactionModel(
    transactionId = transactionId,
    type = type,
    amount = amount,
    description = description,
    date = date,
    categoryId = categoryId,
    accountId = accountId
)
