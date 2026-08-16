package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import kotlinx.datetime.LocalDateTime

data class Transaction(
    val transactionId: TransactionId,
    val type: TransactionType,
    val amount: Money,
    val description: String,
    val occurredAt: LocalDateTime,
    val accountId: AccountId,
    val categoryId: CategoryId?,
) {

    companion object {

        val Empty = Transaction(
            transactionId = TransactionId(""),
            type = TransactionType.Income,
            amount = Money.Zero,
            description = "",
            occurredAt = LocalDateTime(1970, 1, 1, 0, 0),
            accountId = AccountId(""),
            categoryId = null,
        )
    }
}
