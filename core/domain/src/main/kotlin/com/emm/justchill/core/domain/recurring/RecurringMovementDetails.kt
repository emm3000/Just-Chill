package com.emm.justchill.core.domain.recurring

import com.emm.justchill.core.domain.shared.Money

data class RecurringMovementDetails(
    val id: String,
    val name: String,
    val type: com.emm.justchill.core.domain.transaction.TransactionType,
    val amount: Money?,
    val categoryName: String?,
    val categoryColor: String?,
    val accountName: String?,
    val dayOfMonth: Int,
    val isActive: Boolean,
)
