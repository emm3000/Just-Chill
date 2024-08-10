package com.emm.justchill.hh.domain.transaction

import com.emm.justchill.Transactions
import kotlinx.coroutines.flow.Flow

class TransactionFinder(private val repository: TransactionRepository) {

    fun find(transactionId: String): Flow<Transactions?> {
        return repository.find(transactionId)
    }
}