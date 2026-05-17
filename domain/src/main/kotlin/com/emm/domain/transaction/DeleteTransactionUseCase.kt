package com.emm.domain.transaction

import com.emm.domain.shared.TransactionId

class DeleteTransactionUseCase(
    private val repository: TransactionRepository,
) {

    suspend operator fun invoke(transactionId: TransactionId) {
        repository.delete(transactionId)
    }
}
