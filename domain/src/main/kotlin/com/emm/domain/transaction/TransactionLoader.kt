package com.emm.domain.transaction

import kotlinx.coroutines.flow.Flow

class TransactionLoader(private val repository: TransactionRepository) {

    fun load(): Flow<List<Transaction>> {
        return repository.retrieve()
    }
}