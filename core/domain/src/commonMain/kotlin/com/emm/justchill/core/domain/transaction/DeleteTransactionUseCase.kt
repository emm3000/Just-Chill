package com.emm.justchill.core.domain.transaction

import com.emm.justchill.core.domain.shared.TransactionId

class DeleteTransactionUseCase(private val repository: TransactionRepository) {

    suspend operator fun invoke(transactionId: TransactionId) {
        repository.delete(transactionId)
    }
}
