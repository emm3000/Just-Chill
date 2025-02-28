package com.emm.domain.transaction

import kotlinx.coroutines.flow.Flow

class TransactionSumSpend(private val repository: TransactionRepository) {

    operator fun invoke(accountId: String): Flow<Double> {
        return repository.sumSpend(accountId)
    }
}