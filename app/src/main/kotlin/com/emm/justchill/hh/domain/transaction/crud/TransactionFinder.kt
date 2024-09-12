package com.emm.justchill.hh.domain.transaction.crud

import com.emm.justchill.hh.domain.transaction.TransactionRepository
import com.emm.justchill.hh.domain.transaction.model.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionFinder(private val repository: TransactionRepository) {

    fun find(transactionId: String): Flow<Transaction?> {
        return repository.findBy(transactionId)
    }
}