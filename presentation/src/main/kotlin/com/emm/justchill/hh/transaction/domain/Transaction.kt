package com.emm.justchill.hh.transaction.domain

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
}