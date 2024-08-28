package com.emm.justchill.hh.presentation.transaction

import com.emm.justchill.Transactions
import com.emm.justchill.hh.domain.TransactionType
import com.emm.justchill.hh.domain.transaction.fromCentsToSolesWith
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

private fun Transactions.toUi(): TransactionUi {
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

fun List<Transactions>.toUi() = map(Transactions::toUi)