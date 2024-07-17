package com.emm.justchill.hh.presentation.transaction

import com.emm.justchill.Transactions
import com.emm.justchill.hh.domain.TransactionType
import com.emm.justchill.hh.domain.transaction.fromCentsToSoles
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

    return TransactionUi(
        transactionId = transactionId,
        type = transactionType,
        amount = when (transactionType) {
            TransactionType.INCOME -> "S/ ${fromCentsToSoles(amount)}"
            TransactionType.SPENT -> "S/ -${fromCentsToSoles(amount)}"
        },
        description = description,
        date = date,
        readableDate = DateUtils.millisToReadableFormat(date),
        readableTime = if (time == 0L) {
            "00:00 a. m"
        } else {
            DateUtils.readableTime(time)
        }
    )
}

fun List<Transactions>.toUi() = map(Transactions::toUi)