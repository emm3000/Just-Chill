package com.emm.justchill.experiences.hh.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.justchill.TransactionQueries
import com.emm.justchill.Transactions
import com.emm.justchill.core.DispatchersProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TransactionDiskDataSource(
    dispatchersProvider: DispatchersProvider,
    private val transactionQueries: TransactionQueries,
) : TransactionSaver, AllItemsRetriever<Transactions>, DispatchersProvider by dispatchersProvider {

    override suspend fun save(entity: TransactionInsert) = withContext(ioDispatcher) {
        checkNotNull(entity.id)
        transactionQueries.addTransaction(
            transactionId = entity.id,
            type = entity.type,
            mounth = entity.mount,
            description = entity.description,
            date = entity.date,
        )
    }

    override fun retrieve(): Flow<List<Transactions>> {
        return transactionQueries.retrieveAll().asFlow().mapToList(ioDispatcher)
    }
}