package com.emm.justchill.hh.domain.transaction

import com.emm.justchill.Ga
import kotlinx.coroutines.flow.Flow

class TransactionFinder(private val repository: TransactionRepository) {

    fun find(transactionId: String): Flow<Ga?> {
        return repository.find(transactionId)
    }
}