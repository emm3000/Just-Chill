package com.emm.justchill.hh.transaction.presentation

import com.emm.justchill.hh.shared.fromCentsToSolesWith
import com.emm.justchill.hh.transaction.domain.Transaction
import com.google.firebase.crashlytics.FirebaseCrashlytics

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
    val transactionType: TransactionType = try {
        TransactionType.valueOf(type)
    } catch (e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
        TransactionType.INCOME
    }

    val formattedNumber: String = fromCentsToSolesWith(amount)
    return TransactionUi(
        transactionId = transactionId,
        type = transactionType,
        amount = when (transactionType) {
            TransactionType.INCOME -> "S/ $formattedNumber"
            TransactionType.SPENT -> "S/ -$formattedNumber"
        },
        description = description,
        date = date,
        readableDate = DateUtils.millisToReadableFormat(date),
        readableTime = DateUtils.readableTime(date)
    )
}

fun List<Transaction>.toUi() = map(Transaction::toUi)