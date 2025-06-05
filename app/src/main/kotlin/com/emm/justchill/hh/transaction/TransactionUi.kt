package com.emm.justchill.hh.transaction

import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.hh.shared.fromCentsToSolesWith

data class TransactionUi(
    val transactionId: String,
    val type: TransactionType,
    val amount: String,
    val description: String,
    val date: Long,
    val readableDate: String,
    val readableTime: String,
)

private fun Transaction.toUi(): TransactionUi {
    val formattedNumber: String = fromCentsToSolesWith(amount)
    return TransactionUi(
        transactionId = transactionId,
        type = type,
        amount = when (type) {
            TransactionType.Income -> "S/ $formattedNumber"
            TransactionType.Spend -> "S/ -$formattedNumber"
        },
        description = description,
        date = date,
        readableDate = DateUtils.millisToReadableFormat(date),
        readableTime = DateUtils.readableTime(date)
    )
}

fun List<Transaction>.toUi() = map(Transaction::toUi)