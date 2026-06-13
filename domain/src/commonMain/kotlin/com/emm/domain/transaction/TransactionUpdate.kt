package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money

data class TransactionUpdate(
    val type: TransactionType,
    val amount: Money,
    val description: String,
    val accountId: AccountId,
    val categoryId: CategoryId?,
    val date: Long,
)
