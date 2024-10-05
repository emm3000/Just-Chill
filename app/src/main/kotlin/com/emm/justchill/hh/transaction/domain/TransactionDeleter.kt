package com.emm.justchill.hh.transaction.domain

class TransactionDeleter(
    private val updateRepository: TransactionUpdateRepository,
) {

    suspend fun delete(transactionId: String) {
        updateRepository.updateStatus(transactionId, SyncStatus.PENDING_DELETE)
    }
}