package com.emm.justchill.hh.domain.transaction.operations

import com.emm.justchill.hh.domain.transaction.TransactionRepository
import kotlinx.coroutines.flow.Flow

class TransactionSumSpend(private val repository: TransactionRepository) {

    operator fun invoke(): Flow<Double> {
        return repository.sumSpend()
    }
}