package com.emm.justchill.experiences.hh.data.transaction

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.justchill.TransactionQueries
import com.emm.justchill.Transactions
import com.emm.justchill.core.DispatchersProvider
import com.emm.justchill.experiences.hh.data.AllTransactionsRetriever
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TransactionDiskDataSource(
    dispatchersProvider: DispatchersProvider,
    private val transactionQueries: TransactionQueries,
) : TransactionSaver, AllTransactionsRetriever, DispatchersProvider by dispatchersProvider {

    override suspend fun save(entity: TransactionInsert) = withContext(ioDispatcher) {
        checkNotNull(entity.id)
        transactionQueries.addTransaction(
            transactionId = entity.id,
            type = entity.type,
            amount = entity.amount,
            description = entity.description,
            date = entity.date,
        )
    }

    override fun retrieve(): Flow<List<Transactions>> {
        return transactionQueries.retrieveAll().asFlow().mapToList(ioDispatcher)
    }

    override fun retrieveByDateRange(
        startDateMillis: Long,
        endDateMillis: Long
    ): Flow<List<Transactions>> {
        return transactionQueries
            .selectTransactionsByDateRange(startDateMillis, endDateMillis)
            .asFlow()
            .mapToList(ioDispatcher)
    }
}