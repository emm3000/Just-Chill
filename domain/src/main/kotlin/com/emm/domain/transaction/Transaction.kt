package com.emm.domain.transaction

import com.emm.domain.shared.currentTimeInMillis
import java.text.DecimalFormat

data class Transaction(
    val transactionId: String,
    val type: String,
    val amount: Double,
    val description: String,
    val date: Long,
    val accountId: String,
    val categoryId: String?,
) {

    val amountDecimalFormat: String
        get() {
            val decimalFormat = DecimalFormat("#,##0.00")
            return decimalFormat.format(amount)
        }

    companion object {

        val Empty = Transaction(
            transactionId = "",
            type = "",
            amount = 0.0,
            description = "",
            date = currentTimeInMillis(),
            accountId = "",
            categoryId = null
        )
    }
}