package com.emm.justchill.hh.domain.transaction.crud

import com.emm.justchill.hh.domain.transaction.SyncStatus
import com.emm.justchill.hh.domain.transaction.TransactionUpdateRepository

class TransactionDeleter(
    private val updateRepository: TransactionUpdateRepository,
) {

    suspend fun delete(transactionId: String) {
        updateRepository.updateStatus(transactionId, SyncStatus.PENDING_DELETE)
    }
}