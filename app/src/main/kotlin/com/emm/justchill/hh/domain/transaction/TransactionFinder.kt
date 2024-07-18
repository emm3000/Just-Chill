package com.emm.justchill.hh.domain.transaction

import com.emm.justchill.Ga

class TransactionFinder(private val repository: TransactionRepository) {

    suspend fun find(transactionId: String): Ga? {
        return repository.find(transactionId)
    }
}