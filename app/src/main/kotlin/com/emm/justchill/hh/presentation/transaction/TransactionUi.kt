package com.emm.justchill.hh.presentation.transaction

import androidx.compose.ui.graphics.Color
import com.emm.justchill.Transactions
import com.emm.justchill.hh.domain.TransactionType
import com.emm.justchill.hh.domain.transaction.fromCentsToSoles
import com.emm.justchill.hh.domain.transaction.fromCentsToSolesWith
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlin.random.Random

data class TransactionUi(
    val transactionId: String,
    val type: TransactionType,
    val amount: String,
    val description: String,
    val date: Long,
    val readableDate: String,
    val readableTime: String,
    val randomColor: Color = Color(
        Random.nextFloat(),
        Random.nextFloat(),
        Random.nextFloat(),
        Random.nextFloat(),
    )
)

private fun Transactions.toUi(): TransactionUi {
    val transactionType: TransactionType = try {
        TransactionType.valueOf(type)
    } catch (e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
        TransactionType.INCOME
    }

    val amountFormated: String = fromCentsToSolesWith(fromCentsToSoles(amount))
    return TransactionUi(
        transactionId = transactionId,
        type = transactionType,
        amount = when (transactionType) {
            TransactionType.INCOME -> "S/ $amountFormated"
            TransactionType.SPENT -> "S/ -$amountFormated"
        },
        description = description,
        date = date,
        readableDate = DateUtils.millisToReadableFormat(date),
        readableTime = DateUtils.readableTime(date)
    )
}

fun List<Transactions>.toUi() = map(Transactions::toUi)