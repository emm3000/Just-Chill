package com.emm.domain.transaction

import com.emm.domain.category.Category
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.TransactionId

data class TransactionWithCategory(
    val transactionId: TransactionId,
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val date: Long,
    val accountId: AccountId,
    val category: Category?,
)