package com.emm.justchill.hh.transaction.domain

import kotlinx.coroutines.flow.Flow

class TransactionFinder(private val repository: TransactionRepository) {

    fun find(transactionId: String): Flow<Transaction?> {
        return repository.findBy(transactionId)
    }
}