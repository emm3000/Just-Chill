package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.TransactionId
import com.emm.domain.shared.currentTimeInMillis
import java.text.DecimalFormat

data class Transaction(
    val transactionId: TransactionId,
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val date: Long,
    val accountId: AccountId,
    val categoryId: CategoryId?,
) {

    val amountDecimalFormat: String
        get() {
            val decimalFormat = DecimalFormat("#,##0.00")
            return decimalFormat.format(amount)
        }

    companion object {

        val Empty = Transaction(
            transactionId = TransactionId(""),
            type = TransactionType.Income,
            amount = 0.0,
            description = "",
            date = currentTimeInMillis(),
            accountId = AccountId(""),
            categoryId = null
        )
    }
}