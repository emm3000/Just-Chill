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
)

private fun Transactions.toUi() = TransactionUi(
    transactionId = transactionId,
    type = try {
        TransactionType.valueOf(type)
    } catch (e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
        TransactionType.INCOME
    },
    amount = "S/. ${fromCentsToSoles(amount)}",
    description = description,
    date = date,
    readableDate = DateUtils.millisToReadableFormat(date)
)

fun List<Transactions>.toUi() = map(Transactions::toUi)