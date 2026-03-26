package com.emm.domain.transaction

import kotlinx.coroutines.flow.Flow

class GetTransactionsUseCase(private val repository: TransactionRepository) {

    operator fun invoke(): Flow<List<Transaction>> {
        return repository.all()
    }
}
