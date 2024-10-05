package com.emm.justchill.hh.transaction.domain

import kotlinx.coroutines.flow.Flow

class TransactionDifferenceCalculator(private val repository: TransactionRepository) {

    fun calculate(accountId: String): Flow<Double> {
        return repository.difference(accountId)
    }
}