package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId

data class TransactionUpdate(
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val accountId: AccountId,
    val categoryId: CategoryId?,
    val date: Long,
)
