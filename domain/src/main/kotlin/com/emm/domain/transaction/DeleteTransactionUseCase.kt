package com.emm.domain.transaction

import com.emm.domain.shared.TransactionId

class DeleteTransactionUseCase(
    private val updateRepository: TransactionRepository,
) {

    suspend operator fun invoke(transactionId: TransactionId) {
        updateRepository.delete(transactionId)
    }
}
