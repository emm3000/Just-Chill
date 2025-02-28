package com.emm.justchill.hh.transaction.domain

import com.emm.domain.transaction.TransactionRepository
import kotlinx.coroutines.flow.Flow

class TransactionSumSpend(private val repository: TransactionRepository) {

    operator fun invoke(accountId: String): Flow<Double> {
        return repository.sumSpend(accountId)
    }
}