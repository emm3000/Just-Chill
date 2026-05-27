package com.emm.domain.recurring

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.transaction.TransactionType

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
)
