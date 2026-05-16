package com.emm.domain.transaction

class DeleteTransactionUseCase(
    private val updateRepository: TransactionRepository,
) {

    suspend operator fun invoke(transactionId: String) {
        updateRepository.delete(transactionId)
    }
}
