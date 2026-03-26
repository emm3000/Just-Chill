package com.emm.data.transaction

import com.emm.data.Transactions
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionType

// SQLDelight -> Entity (internal, stays within data source)
fun Transactions.asEntity() = TransactionEntity(
    transactionId = transactionId,
    type = type,
    amount = amount,
    description = description,
    date = date,
    categoryId = categoryId,
    accountId = accountId,
    syncState = syncState,
    isDeleted = isDeleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun List<Transactions>.asEntity() = map(Transactions::asEntity)

// Entity -> Domain
fun TransactionEntity.asExternalModel() = Transaction(
    transactionId = transactionId,
    type = TransactionType.valueOf(type),
    amount = amount,
    description = description,
    date = date,
    categoryId = categoryId,
    accountId = accountId,
)

fun List<TransactionEntity>.asExternalModel() = map(TransactionEntity::asExternalModel)

// Domain insert -> Entity
fun TransactionInsert.asEntity() = TransactionEntity(
    transactionId = id,
    type = type.name,
    amount = amount,
    description = description,
    date = date,
    categoryId = categoryId,
    accountId = accountId,
    syncState = "",
    isDeleted = false,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// Network -> Entity
fun NetworkTransaction.asEntity() = TransactionEntity(
    transactionId = transactionId,
    type = type,
    amount = amount,
    description = description,
    date = date,
    categoryId = categoryId,
    accountId = accountId,
    syncState = "",
    isDeleted = deleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// Entity -> Network
fun TransactionEntity.asNetworkModel(userId: String) = NetworkTransaction(
    transactionId = transactionId,
    type = type,
    amount = amount,
    description = description,
    date = date,
    updatedAt = updatedAt,
    createdAt = createdAt,
    deleted = isDeleted,
    categoryId = categoryId,
    accountId = accountId,
    userId = userId,
)
