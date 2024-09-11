package com.emm.justchill.hh.domain.transaction

import com.emm.justchill.hh.domain.transaction.model.TransactionUpdate

interface TransactionUpdateRepository {

    suspend fun update(
        transactionId: String,
        transactionUpdate: TransactionUpdate,
    )

    suspend fun updateStatus(transactionId: String, syncStatus: SyncStatus)
}