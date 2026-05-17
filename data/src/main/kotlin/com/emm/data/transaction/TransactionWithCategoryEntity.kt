package com.emm.data.transaction

data class TransactionWithCategoryEntity(
    val transactionId: String,
    val type: String,
    val amount: Long,
    val description: String,
    val date: Long,
    val accountId: String,
    val categoryId: String?,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColor: String?,
    val categoryType: String?,
)
