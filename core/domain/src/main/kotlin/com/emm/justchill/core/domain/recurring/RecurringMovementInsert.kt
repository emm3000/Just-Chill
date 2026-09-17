package com.emm.justchill.core.domain.recurring

import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.transaction.TransactionType

data class RecurringMovementInsert(
    val name: String,
    val type: TransactionType,
    val amount: Money?,
    val description: String = "",
    val categoryId: CategoryId?,
    val accountId: AccountId,
    val dayOfMonth: Int,
    val isActive: Boolean = true,
)
