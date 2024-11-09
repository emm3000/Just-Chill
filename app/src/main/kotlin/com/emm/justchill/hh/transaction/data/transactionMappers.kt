package com.emm.justchill.hh.transaction.data

import com.emm.justchill.Transactions
import com.emm.justchill.hh.transaction.domain.Transaction

fun Transactions.toDomain(): Transaction = Transaction(
    transactionId = transactionId,
    type = type,
    amount = amount,
    description = description,
    date = date,
    categoryId = categoryId,
    accountId = accountId,
)

fun List<Transactions>.toDomain(): List<Transaction> = map(Transactions::toDomain)