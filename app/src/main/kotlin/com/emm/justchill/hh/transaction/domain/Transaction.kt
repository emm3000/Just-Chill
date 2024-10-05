package com.emm.justchill.hh.transaction.domain

data class Transaction(
    val transactionId: String,
    val type: String,
    val amount: Double,
    val description: String,
    val date: Long,
    val accountId: String,
    val categoryId: String?,
    val syncStatus: String,
)