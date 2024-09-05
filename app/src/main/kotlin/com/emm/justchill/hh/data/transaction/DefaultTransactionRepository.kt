package com.emm.justchill.hh.data.transaction

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.justchill.TransactionQueries
import com.emm.justchill.Transactions
import com.emm.justchill.core.DispatchersProvider
import com.emm.justchill.hh.domain.transaction.TransactionModel
import com.emm.justchill.hh.domain.auth.AuthRepository
import com.emm.justchill.hh.domain.transaction.toModel
import com.emm.justchill.hh.domain.transaction.TransactionRepository
import com.emm.justchill.hh.domain.transaction.remote.TransactionSupabaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultTransactionRepository(
    dispatchersProvider: DispatchersProvider,
    private val transactionsQueries: TransactionQueries,
    private val networkDataSource: TransactionSupabaseRepository,
    private val authRepository: AuthRepository,
) : TransactionRepository, DispatchersProvider by dispatchersProvider {

    override suspend fun add(entity: TransactionInsert) = withContext(ioDispatcher) {
        checkNotNull(entity.id)
        transactionsQueries.addTransaction(
            transactionId = entity.id,
            type = entity.type,
            amount = entity.amount,
            description = entity.description,
            date = entity.date,
            syncStatus = "",
        )
    }

    override fun all(): Flow<List<Transactions>> {
        return transactionsQueries
            .retrieveAll()
            .asFlow()
            .mapToList(ioDispatcher)
    }

    override fun retrieveWithLimit(limit: Long, offset: Long): Flow<List<Transactions>> {
        return transactionsQueries
            .retrieveWithPaging(limit, offset)
            .asFlow()
            .mapToList(ioDispatcher)
    }

    override fun retrieveByDateRange(
        startDateMillis: Long,
        endDateMillis: Long
    ): Flow<List<Transactions>> {
        return transactionsQueries
            .selectTransactionsByDateRange(startDateMillis, endDateMillis)
            .asFlow()
            .mapToList(ioDispatcher)
    }

    override fun retrieveByDateRangeWithLimit(
        startDateMillis: Long,
        endDateMillis: Long,
        limit: Long,
        offset: Long
    ): Flow<List<Transactions>> {
        return transactionsQueries
            .selectTransactionsByDateRangeWithPaging(
                startDateMillis,
                endDateMillis,
                limit,
                offset
            )
            .asFlow()
            .mapToList(ioDispatcher)
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

    override suspend fun delete(transactionId: String) = withContext(ioDispatcher) {
        transactionsQueries.delete(transactionId)
    }

    override suspend fun seed() {
        val transactions: List<TransactionModel> = networkDataSource.retrieve()
        transactionsQueries.transaction {
            populate(transactions)
        }
    }

    override suspend fun backup() {
        val authId: String = authRepository.session()?.id ?: return
        networkDataSource.deleteAll()
        val transactions: List<TransactionModel> = transactionsQueries
            .retrieveAll()
            .executeAsList()
            .map(Transactions::toModel)
            .map {
                it.copy(
                    userId = authId,
                )
            }

        networkDataSource.upsert(transactions)
    }

    override fun find(transactionId: String): Flow<Transactions?> {
        return transactionsQueries.find(transactionId)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
    }

    override suspend fun update(
        transactionId: String,
        transactionUpdate: TransactionUpdate,
    ) = withContext(ioDispatcher) {
        transactionsQueries.updateValues(
            type = transactionUpdate.type,
            amount = transactionUpdate.amount,
            description = transactionUpdate.description,
            date = transactionUpdate.date,
            transactionId = transactionId
        )
    }

    private fun populate(data: List<TransactionModel>) {
        data.forEach {
            transactionsQueries.addTransaction(
                transactionId = it.transactionId,
                type = it.type,
                amount = it.amount,
                description = it.description,
                date = it.date,
                syncStatus = "",
            )
        }
    }
}