package com.emm.justchill.core.domain.recurring

import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.RecurringMovementId
import com.emm.justchill.core.domain.transaction.TransactionType

data class RecurringMovement(
    val id: RecurringMovementId,
    val name: String,
    val type: TransactionType,
    val amount: Money?,
    val description: String,
    val categoryId: CategoryId?,
    val accountId: AccountId,
    val frequency: Frequency,
    val dayOfMonth: Int,
    val isActive: Boolean,
    val lastConfirmedPeriod: String?,
    val createdAt: Long,
)
