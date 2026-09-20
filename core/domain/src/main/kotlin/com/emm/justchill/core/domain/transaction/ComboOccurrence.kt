package com.emm.justchill.core.domain.transaction

import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money

data class ComboOccurrence(
    val accountId: AccountId,
    val categoryId: CategoryId,
    val type: TransactionType,
    val amount: Money,
    val occurredAt: String,
)
