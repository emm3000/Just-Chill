package com.emm.data.transaction

data class TransactionEntity(
    val transactionId: String,
    val type: String,
    val amount: Long,
    val description: String,
    val occurredAt: String,
    val categoryId: String?,
    val accountId: String,
    val createdAt: Long,
    val updatedAt: Long,
)
