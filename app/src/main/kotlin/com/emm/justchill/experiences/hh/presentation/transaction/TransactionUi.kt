package com.emm.justchill.experiences.hh.presentation.transaction

import com.emm.justchill.Transactions
import com.emm.justchill.experiences.hh.domain.TransactionType

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
    } catch (e: Exception) {
        TransactionType.INCOME
    },
    amount = amount,
    description = description,
    date = date,
    readableDate = DateUtils.millisToReadableFormat(date)
)

fun List<Transactions>.toUi() = map(Transactions::toUi)