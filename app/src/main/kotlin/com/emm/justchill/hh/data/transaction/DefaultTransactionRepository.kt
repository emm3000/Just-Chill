package com.emm.justchill.hh.data.transaction

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.justchill.TransactionQueries
import com.emm.justchill.Transactions
import com.emm.justchill.core.DispatchersProvider
import com.emm.justchill.hh.domain.AndroidDataProvider
import com.emm.justchill.hh.domain.TransactionModel
import com.emm.justchill.hh.domain.auth.AuthRepository
import com.emm.justchill.hh.domain.toModel
import com.emm.justchill.hh.domain.transaction.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultTransactionRepository(
    dispatchersProvider: DispatchersProvider,
    private val transactionsQueries: TransactionQueries,
    private val networkDataSource: TransactionNetworkDataSource,
    private val androidDataProvider: AndroidDataProvider,
    private val authRepository: AuthRepository,
) : TransactionRepository, DispatchersProvider by dispatchersProvider {

    override suspend fun add(entity: TransactionInsert) {
        checkNotNull(entity.id)
        transactionsQueries.addTransaction(
            transactionId = entity.id,
            type = entity.type,
            amount = entity.amount,
            description = entity.description,
            date = entity.date,
            time = entity.time,
        )
    }

    override fun all(): Flow<List<Transactions>> {
        return transactionsQueries
            .retrieveAll()
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

    override fun sumIncome(): Flow<Long> {
        return transactionsQueries.sumAllIncomeAmounts()
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.totalIncome ?: 0L }
    }

    override fun sumSpend(): Flow<Long> {
        return transactionsQueries.sumAllSpendAmounts()
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.totalIncome ?: 0L }
    }

    override fun difference(): Flow<Long> {
        return transactionsQueries.difference()
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it ?: 0L }
    }

    override suspend fun seed() {
        val transactions: List<TransactionModel> = networkDataSource.retrieve()
        transactionsQueries.transaction {
            populate(transactions)
        }
    }

    override suspend fun backup() {
        val authId: String = authRepository.session()?.id ?: return
        val transactions: List<TransactionModel> = transactionsQueries
            .retrieveAll()
            .executeAsList()
            .map(Transactions::toModel)
            .map {
                it.copy(
                    deviceId = androidDataProvider.androidId(),
                    deviceName = androidDataProvider.deviceName(),
                    userId = authId,
                )
            }

        networkDataSource.upsert(transactions)
    }

    private fun populate(data: List<TransactionModel>) {
        data.forEach {
            transactionsQueries.addTransaction(
                transactionId = it.transactionId,
                type = it.type,
                amount = it.amount,
                description = it.description,
                date = it.date,
                time = it.time,
            )
        }
    }
}