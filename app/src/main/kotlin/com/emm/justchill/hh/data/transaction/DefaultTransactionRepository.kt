package com.emm.justchill.hh.data.transaction

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.justchill.TransactionQueries
import com.emm.justchill.Transactions
import com.emm.justchill.core.DispatchersProvider
import com.emm.justchill.hh.data.shared.Syncer
import com.emm.justchill.hh.domain.transaction.SyncStatus
import com.emm.justchill.hh.domain.transaction.model.TransactionInsert
import com.emm.justchill.hh.domain.transaction.TransactionRepository
import com.emm.justchill.hh.domain.transaction.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultTransactionRepository(
    dispatchersProvider: DispatchersProvider,
    private val transactionsQueries: TransactionQueries,
    private val syncer: Syncer,
) : TransactionRepository, DispatchersProvider by dispatchersProvider {

    override suspend fun create(transactionInsert: TransactionInsert) = withContext(ioDispatcher) {
        checkNotNull(transactionInsert.id)
        transactionsQueries.addTransaction(
            transactionId = transactionInsert.id,
            type = transactionInsert.type,
            amount = transactionInsert.amount,
            description = transactionInsert.description,
            date = transactionInsert.date,
            syncStatus = SyncStatus.PENDING_INSERT.name,
            categoryId = transactionInsert.categoryId,
            accountId = transactionInsert.accountId
        )
        syncer.sync(transactionInsert.id)
    }

    override fun retrieve(): Flow<List<Transaction>> {
        return transactionsQueries
            .retrieveAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map(List<Transactions>::toDomain)
    }

    override fun sumIncome(): Flow<Double> {
        return transactionsQueries.sumAllIncomeAmounts()
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.totalIncome ?: 0.0 }
    }

    override fun sumSpend(): Flow<Double> {
        return transactionsQueries.sumAllSpendAmounts()
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.totalIncome ?: 0.0 }
    }

    override fun difference(): Flow<Double> {
        return transactionsQueries.difference()
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it ?: 0.0 }
    }

    override suspend fun deleteBy(transactionId: String) = withContext(ioDispatcher) {
        transactionsQueries.delete(transactionId)
    }

    override fun findBy(transactionId: String): Flow<Transaction?> {
        return transactionsQueries.find(transactionId)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.let(Transactions::toDomain) }
    }
}