package com.emm.domain.transaction

import kotlinx.coroutines.flow.Flow

class TransactionFinder(private val repository: TransactionRepository) {

    fun find(transactionId: String): Flow<Transaction?> {
        return repository.find(transactionId)
    }
}