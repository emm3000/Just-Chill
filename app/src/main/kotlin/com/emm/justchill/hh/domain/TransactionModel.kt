package com.emm.justchill.hh.domain

import com.emm.justchill.Transactions
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionModel(

    @SerialName("transaction_id")
    val transactionId: String,

    val type: String,
    val amount: Long,
    val description: String,
    val date: Long,
    val time: Long,

    @SerialName("device_id")
    val deviceId: String = "",

    @SerialName("device_name")
    val deviceName: String = "",

    @SerialName("user_id")
    val userId: String = "",
)

fun Transactions.toModel() = TransactionModel(
    transactionId = transactionId,
    type = type,
    amount = amount,
    description = description,
    date = date,
    deviceId = "",
    time = time
)
