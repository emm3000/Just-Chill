package com.emm.justchill.hh.domain

import com.emm.justchill.TransactionsCategories
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionCategoryModel(

    @SerialName("transaction_id")
    val transactionId: String,

    @SerialName("category_id")
    val categoryId: String,

    @SerialName("device_id")
    val deviceId: String = "",

    @SerialName("device_name")
    val deviceName: String = "",

    @SerialName("user_id")
    val userId: String = "",
)

fun TransactionsCategories.toModel() = TransactionCategoryModel(
    transactionId = transactionId,
    categoryId = categoryId,
)