package com.emm.domain.transaction

import com.emm.domain.shared.TransactionId

class FindTransactionUseCase(private val repository: TransactionRepository) {

    operator fun invoke(transactionId: TransactionId): Transaction? {
        return repository.find(transactionId)
    }
}
