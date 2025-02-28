package com.emm.domain.transaction

import kotlinx.coroutines.flow.Flow

class TransactionSumIncome(private val repository: TransactionRepository) {

    operator fun invoke(accountId: String): Flow<Double> {
        return repository.sumIncome(accountId)
    }
}