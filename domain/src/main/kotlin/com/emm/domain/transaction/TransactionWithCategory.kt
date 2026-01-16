package com.emm.domain.transaction

import com.emm.domain.category.Category

data class TransactionWithCategory(
    val transactionId: String,
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val date: Long,
    val accountId: String,
    val category: Category?,
)