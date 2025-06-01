package com.emm.data.transaction

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.data.Transactions
import com.emm.data.TransactionsQueries
import com.emm.data.currentTimeInMillis
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TransactionLocalDataSource(private val transactionsQueries: TransactionsQueries) {

    suspend fun create(transactionInsert: TransactionInsert) = withContext(Dispatchers.IO) {
        checkNotNull(transactionInsert.id)
        transactionsQueries.insert(
            transactionId = transactionInsert.id!!,
            type = transactionInsert.type.name,
            amount = transactionInsert.amount,
            description = transactionInsert.description,
            date = transactionInsert.date,
            categoryId = transactionInsert.categoryId,
            accountId = transactionInsert.accountId,
            updatedAt = currentTimeInMillis(),
        )
    }

    fun retrieve(): Flow<List<Transaction>> {
        return transactionsQueries
            .all()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map(List<Transactions>::toDomain)
    }

    fun sumIncome(accountId: String): Flow<Double> {
        return transactionsQueries.sumAllIncomeAmounts(accountId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.totalIncome ?: 0.0 }
    }

    fun sumSpend(accountId: String): Flow<Double> {
        return transactionsQueries.sumAllSpendAmounts(accountId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.totalIncome ?: 0.0 }
    }

    fun difference(accountId: String): Flow<Double> {
        return transactionsQueries.difference(accountId, accountId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it ?: 0.0 }
    }

    suspend fun delete(transactionId: String) = withContext(Dispatchers.IO) {
        transactionsQueries.delete(transactionId)
    }

    fun find(transactionId: String): Flow<Transaction?> {
        return transactionsQueries.find(transactionId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.let(Transactions::toDomain) }
    }

    suspend fun update(
        transactionId: String,
        transactionUpdate: TransactionUpdate,
    ) = withContext(Dispatchers.IO) {
        transactionsQueries.update(
            type = transactionUpdate.type.name,
            amount = transactionUpdate.amount,
            description = transactionUpdate.description,
            date = transactionUpdate.date,
            transactionId = transactionId,
            accountId = transactionUpdate.accountId,
            updatedAt = currentTimeInMillis(),
        )
    }
}