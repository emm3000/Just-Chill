package com.emm.data.transaction

data class TransactionWithCategoryEntity(
    val transactionId: String,
    val type: String,
    val amount: Long,
    val description: String,
    /** ISO local text, exactly as the column holds it. Parsed once, by the mapper. */
    val occurredAt: String,
    val accountId: String,
    val categoryId: String?,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColor: String?,
    val categoryType: String?,
)
