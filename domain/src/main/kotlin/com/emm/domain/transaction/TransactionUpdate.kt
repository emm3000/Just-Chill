package com.emm.domain.transaction

data class TransactionUpdate(
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val accountId: String,
    val categoryId: String?,
    val date: Long,
)
