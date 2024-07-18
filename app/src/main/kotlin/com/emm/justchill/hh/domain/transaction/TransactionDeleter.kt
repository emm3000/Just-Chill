package com.emm.justchill.hh.domain.transaction

class TransactionDeleter(private val repository: TransactionRepository) {

    suspend fun delete(transactionId: String) {
        repository.delete(transactionId = transactionId)
    }
}