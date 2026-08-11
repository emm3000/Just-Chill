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
    /**
     * When the money moved, as the user means it: a calendar day and a wall-clock time, with no
     * timezone. This is the whole of a transaction's "when" — there is no second field to keep it
     * in agreement with, which is the point.
     */
    val occurredAt: LocalDateTime,
    val accountId: AccountId,
    val categoryId: CategoryId?,
) {

    companion object {

        /**
         * The placeholder every other field of which is already empty — an empty id, a zero amount,
         * a blank description.
         *
         * [occurredAt] is the epoch for the same reason: this is the absence of a transaction, and
         * a constant that reads the wall clock is not a constant. It used to be
         * `currentTimeInMillis()`, so its value depended on when the class initialised — first
         * touch, not first use — and no test could state what `Transaction.Empty` was.
         */
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
