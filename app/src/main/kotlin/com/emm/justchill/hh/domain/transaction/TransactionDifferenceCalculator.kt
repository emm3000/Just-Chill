package com.emm.justchill.hh.domain.transaction

import kotlinx.coroutines.flow.Flow

class TransactionDifferenceCalculator(private val repository: TransactionRepository) {

    fun calculate(): Flow<Double> {
        return repository.difference()
    }
}