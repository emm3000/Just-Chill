package com.emm.justchill.core.domain.transaction

import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.TransactionId
import kotlinx.datetime.LocalDateTime

data class TransactionWithCategory(
    val transactionId: TransactionId,
    val type: TransactionType,
    val amount: Money,
    val description: String,
    val occurredAt: LocalDateTime,
    val accountId: AccountId,
    val accountName: String,
    val category: Category?,
)
