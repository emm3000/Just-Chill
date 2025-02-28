package com.emm.justchill.hh.transaction.domain

import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionRepository
import kotlinx.coroutines.flow.Flow

class TransactionFinder(private val repository: TransactionRepository) {

    fun find(transactionId: String): Flow<Transaction?> {
        return repository.findBy(transactionId)
    }
}