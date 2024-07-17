package com.emm.justchill.hh.domain.transaction

import com.emm.justchill.Transactions

class TransactionFinder(private val repository: TransactionRepository) {

    suspend fun find(transactionId: String): Transactions? {
        return repository.find(transactionId)
    }
}