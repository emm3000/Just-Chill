package com.emm.domain.transaction

import com.emm.domain.category.Category
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import kotlinx.datetime.LocalDateTime

data class TransactionWithCategory(
    val transactionId: TransactionId,
    val type: TransactionType,
    val amount: Money,
    val description: String,
    /** When the money moved: calendar day + wall-clock time, no timezone. See [Transaction]. */
    val occurredAt: LocalDateTime,
    val accountId: AccountId,
    val category: Category?,
)
