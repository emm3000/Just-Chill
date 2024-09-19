package com.emm.justchill.hh.domain.transaction.operations

import com.emm.justchill.hh.domain.transaction.TransactionRepository
import kotlinx.coroutines.flow.Flow

class TransactionDifferenceCalculator(private val repository: TransactionRepository) {

    fun calculate(accountId: String): Flow<Double> {
        return repository.difference(accountId)
    }
}