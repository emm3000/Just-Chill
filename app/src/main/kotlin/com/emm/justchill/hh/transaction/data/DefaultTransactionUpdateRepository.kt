package com.emm.justchill.hh.transaction.data

import com.emm.justchill.TransactionQueries
import com.emm.justchill.hh.shared.Syncer
import com.emm.justchill.hh.transaction.domain.SyncStatus
import com.emm.justchill.hh.transaction.domain.TransactionUpdate
import com.emm.justchill.hh.transaction.domain.TransactionUpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultTransactionUpdateRepository(
    private val transactionQueries: TransactionQueries,
    private val syncer: Syncer,
) : TransactionUpdateRepository {

    override suspend fun update(
        transactionId: String,
        transactionUpdate: TransactionUpdate,
    ) = withContext(Dispatchers.IO) {
        transactionQueries.updateValues(
            type = transactionUpdate.type.name,
            amount = transactionUpdate.amount,
            description = transactionUpdate.description,
            date = transactionUpdate.date,
            transactionId = transactionId,
            accountId = transactionUpdate.accountId,
            syncStatus = SyncStatus.PENDING_UPDATE.name,
        )
//        syncer.sync(transactionId)
    }

    override suspend fun updateStatus(
        transactionId: String,
        syncStatus: SyncStatus,
    ) = withContext(Dispatchers.IO) {
        transactionQueries.updateStatus(syncStatus.name, transactionId)
//        syncer.sync(transactionId)
    }
}