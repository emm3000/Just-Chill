package com.emm.domain.transaction

import kotlinx.coroutines.flow.Flow

class TransactionDifferenceCalculator(private val repository: TransactionRepository) {

    fun calculate(accountId: String): Flow<Double> {
        return repository.difference(accountId)
    }
}