package com.emm.domain.transaction

import com.emm.domain.shared.DateAndTimeCombiner

class TransactionUpdater(
    private val repository: TransactionUpdateRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
) {

    suspend fun update(
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
