package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId

data class Transaction(
    val transactionId: TransactionId,
    val type: TransactionType,
    val amount: Money,
    val description: String,
    val date: Long,
    val accountId: AccountId,
    val categoryId: CategoryId?,
) {

    companion object {

        /**
         * The placeholder every other field of which is already empty — an empty id, a zero amount,
         * a blank description.
         *
         * [date] is the epoch for the same reason: this is the absence of a transaction, and a
         * constant that reads the wall clock is not a constant. It used to be
         * `currentTimeInMillis()`, so its value depended on when the class initialised — first
         * touch, not first use — and no test could state what `Transaction.Empty` was.
         */
        val Empty = Transaction(
            transactionId = TransactionId(""),
            type = TransactionType.Income,
            amount = Money.Zero,
            description = "",
            date = 0L,
            accountId = AccountId(""),
            categoryId = null,
        )
    }
}
