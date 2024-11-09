package com.emm.justchill.hh.transaction.domain

class TransactionDeleter(
    private val updateRepository: TransactionRepository,
) {

    suspend fun delete(transactionId: String) {
        updateRepository.deleteBy(transactionId)
    }
}