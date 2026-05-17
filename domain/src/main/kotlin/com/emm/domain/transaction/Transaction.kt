package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import com.emm.domain.shared.currentTimeInMillis

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

        val Empty = Transaction(
            transactionId = TransactionId(""),
            type = TransactionType.Income,
            amount = Money.Zero,
            description = "",
            date = currentTimeInMillis(),
            accountId = AccountId(""),
            categoryId = null
        )
    }
}