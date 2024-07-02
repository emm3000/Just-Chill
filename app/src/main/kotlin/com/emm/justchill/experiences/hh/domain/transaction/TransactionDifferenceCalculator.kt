package com.emm.justchill.experiences.hh.domain.transaction

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class TransactionDifferenceCalculator(private val repository: TransactionRepository) {

    fun calculate(): Flow<BigDecimal> {
        return repository.difference().map(::fromCentsToSoles)
    }
}