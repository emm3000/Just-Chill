package com.emm.justchill.hh.domain.transaction

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionDifferenceCalculator(private val repository: TransactionRepository) {

    fun calculate(): Flow<String> {
        return repository.difference()
            .map(::fromCentsToSoles)
            .map(::fromCentsToSolesWith)
    }
}