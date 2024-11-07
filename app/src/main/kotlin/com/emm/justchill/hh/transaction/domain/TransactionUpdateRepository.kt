package com.emm.justchill.hh.transaction.domain

interface TransactionUpdateRepository {

    suspend fun update(
        transactionId: String,
        transactionUpdate: TransactionUpdate,
    )

    suspend fun updateStatus(transactionId: String, syncStatus: SyncStatus)
}