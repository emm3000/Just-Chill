package com.emm.data.transaction

import com.emm.data.Transactions
import com.emm.domain.account.Account
import com.emm.domain.shared.SyncState
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionUpdate

fun Transactions.toDomain(): Transaction = Transaction(
    transactionId = transactionId,
    type = TransactionType.valueOf(type),
    amount = amount,
    description = description,
    date = date,
    categoryId = categoryId,
    accountId = accountId,
)

fun List<Transactions>.toDomain(): List<Transaction> = map(Transactions::toDomain)

fun Transactions.toModel() = TransactionModel(
    transactionId = transactionId,
    type = type,
    amount = amount,
    description = description,
    date = date,
    updatedAt = updatedAt,
    deleted = isDeleted,
    categoryId = categoryId,
    createdAt = createdAt,
    accountId = accountId,
)

fun Transactions.toTransactionUpdate(): TransactionUpdate {
    val account = Account(
        accountId = accountId,
        name = "",
        balance = 0.0,
    )
    return TransactionUpdate(
        type = TransactionType.valueOf(type),
        amount = amount,
        description = description,
        account = account,
        date = date,
        syncState = SyncState.Synced,
    )
}

fun TransactionModel.toTransactionInsert() = TransactionInsert(
    id = transactionId,
    type = TransactionType.valueOf(type),
    amount = amount,
    description = description,
    date = date,
    account = Account(
        accountId = accountId,
        name = "",
        balance = 0.0,
    ),
    updatedAt = updatedAt,
    createdAt = createdAt,
    categoryId = categoryId,
)