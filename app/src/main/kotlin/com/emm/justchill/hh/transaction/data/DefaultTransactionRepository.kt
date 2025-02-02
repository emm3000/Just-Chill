package com.emm.justchill.hh.transaction.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.justchill.TransactionQueries
import com.emm.justchill.Transactions
import com.emm.justchill.core.DispatchersProvider
import com.emm.justchill.hh.transaction.domain.TransactionInsert
import com.emm.justchill.hh.transaction.domain.TransactionRepository
import com.emm.justchill.hh.transaction.domain.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultTransactionRepository(
    dispatchersProvider: DispatchersProvider,
    private val transactionsQueries: TransactionQueries,
) : TransactionRepository, DispatchersProvider by dispatchersProvider {

    override suspend fun create(transactionInsert: TransactionInsert) = withContext(ioDispatcher) {
        checkNotNull(transactionInsert.id)
        transactionsQueries.addTransaction(
            transactionId = transactionInsert.id,
            type = transactionInsert.type.name,
            amount = transactionInsert.amount,
            description = transactionInsert.description,
            date = transactionInsert.date,
            categoryId = transactionInsert.categoryId,
            accountId = transactionInsert.accountId
        )
    }

    override fun retrieve(accountId: String): Flow<List<Transaction>> {
        return transactionsQueries
            .retrieveAll(accountId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map(List<Transactions>::toDomain)
    }

    override fun sumIncome(accountId: String): Flow<Double> {
        return transactionsQueries.sumAllIncomeAmounts(accountId)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.totalIncome ?: 0.0 }
    }

    override fun sumSpend(accountId: String): Flow<Double> {
        return transactionsQueries.sumAllSpendAmounts(accountId)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.totalIncome ?: 0.0 }
    }

    override fun difference(accountId: String): Flow<Double> {
        return transactionsQueries.difference(accountId, accountId)
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