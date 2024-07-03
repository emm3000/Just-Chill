package com.emm.justchill.experiences.hh.domain

import com.emm.justchill.TransactionsCategories
import kotlinx.serialization.Serializable

@Serializable
data class TransactionCategoryModel(
    val transactionId: String,
    val categoryId: String,
)

fun TransactionsCategories.toModel() = TransactionCategoryModel(
    transactionId = transactionId,
    categoryId = categoryId,
)