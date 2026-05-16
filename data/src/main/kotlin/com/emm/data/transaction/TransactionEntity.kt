package com.emm.data.transaction

data class TransactionEntity(
    val transactionId: String,
    val type: String,
    val amount: Double,
    val description: String,
    val date: Long,
    val categoryId: String?,
    val accountId: String,
    val createdAt: Long,
    val updatedAt: Long,
)
