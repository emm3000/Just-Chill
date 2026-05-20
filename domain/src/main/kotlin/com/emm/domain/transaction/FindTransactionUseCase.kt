package com.emm.domain.transaction

import com.emm.domain.shared.TransactionId

class FindTransactionUseCase(private val repository: TransactionRepository) {

    suspend operator fun invoke(transactionId: TransactionId): Transaction? = repository.find(transactionId)
}
