package com.emm.domain.transaction

class FindTransactionUseCase(private val repository: TransactionRepository) {

    operator fun invoke(transactionId: String): Transaction? {
        return repository.find(transactionId)
    }
}
