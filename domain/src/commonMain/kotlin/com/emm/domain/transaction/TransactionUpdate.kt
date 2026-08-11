package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import kotlinx.datetime.LocalDateTime

data class TransactionUpdate(
    val type: TransactionType,
    val amount: Money,
    val description: String,
    val accountId: AccountId,
    val categoryId: CategoryId?,
    /**
     * When the money moved, after the edit. Written verbatim — an edit that did not touch the date
     * hands back the value it was loaded with, so it is byte-identical by construction rather than
     * by a conditional. See [Transaction].
     */
    val occurredAt: LocalDateTime,
)
