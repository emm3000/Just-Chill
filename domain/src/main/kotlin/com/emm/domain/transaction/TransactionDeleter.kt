package com.emm.domain.transaction

class TransactionDeleter(
    private val updateRepository: TransactionRepository,
) {

    suspend fun delete(transactionId: String) {
        updateRepository.delete(transactionId)
    }
}
