package com.emm.data.transaction

import com.emm.data.Transactions
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

    @SerialName("updated_at")
    val updatedAt: Long,

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
    updatedAt = updatedAt,
    categoryId = categoryId,
    accountId = accountId,
)
