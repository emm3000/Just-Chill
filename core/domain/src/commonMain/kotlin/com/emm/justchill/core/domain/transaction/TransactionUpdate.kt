package com.emm.justchill.core.domain.transaction

import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
import kotlinx.datetime.LocalDateTime

data class TransactionUpdate(
    val type: TransactionType,
    val amount: Money,
    val description: String,
    val accountId: AccountId,
    val categoryId: CategoryId?,
    val occurredAt: LocalDateTime,
)
