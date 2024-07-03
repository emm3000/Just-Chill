package com.emm.justchill.hh.domain

import com.emm.justchill.Transactions
import kotlinx.serialization.Serializable

@Serializable
data class TransactionModel(
    val transactionId: String,
    val type: String,
    val amount: Long,
    val description: String,
    val date: Long,
)

fun Transactions.toModel() = TransactionModel(
    transactionId = transactionId,
    type = type,
    amount = amount,
    description = description,
    date = date
)
