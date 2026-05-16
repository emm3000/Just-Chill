package com.emm.domain.transaction

import com.emm.domain.shared.DateAndTimeCombiner

class UpdateTransactionUseCase(
    private val repository: TransactionRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
) {

    suspend operator fun invoke(
        oldTransaction: Transaction,
        transactionUpdate: TransactionUpdate,
    ) {
        val dateAndTimeCombined: Long = dateAndTimeCombiner.combineWithUtc(transactionUpdate.date)
        val updatedTransaction: TransactionUpdate = transactionUpdate.copy(date = dateAndTimeCombined)
        repository.update(
            transactionId = oldTransaction.transactionId,
            transactionUpdate = updatedTransaction,
        )
    }
}
