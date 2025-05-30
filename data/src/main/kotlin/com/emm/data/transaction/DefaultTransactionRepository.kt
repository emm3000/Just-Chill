package com.emm.data.transaction

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.data.Transactions
import com.emm.data.TransactionsQueries
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultTransactionRepository(
    private val transactionsQueries: TransactionsQueries,
) : TransactionRepository {

    override suspend fun create(transactionInsert: TransactionInsert) = withContext(Dispatchers.IO) {
        checkNotNull(transactionInsert.id)
        transactionsQueries.insert(
            transactionId = transactionInsert.id!!,
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
            .all(accountId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map(List<Transactions>::toDomain)
    }

    override fun sumIncome(accountId: String): Flow<Double> {
        return transactionsQueries.sumAllIncomeAmounts(accountId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.totalIncome ?: 0.0 }
    }

    override fun sumSpend(accountId: String): Flow<Double> {
        return transactionsQueries.sumAllSpendAmounts(accountId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.totalIncome ?: 0.0 }
    }

    override fun difference(accountId: String): Flow<Double> {
        return transactionsQueries.difference(accountId, accountId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it ?: 0.0 }
    }

    override suspend fun deleteBy(transactionId: String) = withContext(Dispatchers.IO) {
        transactionsQueries.delete(transactionId)
    }

    override fun findBy(transactionId: String): Flow<Transaction?> {
        return transactionsQueries.find(transactionId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.let(Transactions::toDomain) }
    }
}