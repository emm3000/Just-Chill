package com.emm.justchill.hh.transaction.domain

import com.emm.domain.transaction.TransactionRepository

class TransactionDeleter(
    private val updateRepository: TransactionRepository,
) {

    suspend fun delete(transactionId: String) {
        updateRepository.deleteBy(transactionId)
    }
}