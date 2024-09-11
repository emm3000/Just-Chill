package com.emm.justchill.hh.data.transaction

import com.emm.justchill.Transactions
import com.emm.justchill.hh.domain.transaction.model.Transaction

fun Transactions.toDomain(): Transaction = Transaction(
    transactionId = transactionId,
    type = type,
    amount = amount,
    description = description,
    date = date,
    syncStatus = syncStatus,
)

fun List<Transactions>.toDomain(): List<Transaction> = map(Transactions::toDomain)