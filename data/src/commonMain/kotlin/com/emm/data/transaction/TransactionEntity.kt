package com.emm.data.transaction

data class TransactionEntity(
    val transactionId: String,
    val type: String,
    val amount: Long,
    val description: String,
    /** ISO local text, exactly as the column holds it. Parsed once, by the mapper. */
    val occurredAt: String,
    val categoryId: String?,
    val accountId: String,
    val createdAt: Long,
    val updatedAt: Long,
)
