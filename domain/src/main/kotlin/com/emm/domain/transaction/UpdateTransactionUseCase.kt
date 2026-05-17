package com.emm.domain.transaction

import com.emm.domain.shared.DateAndTimeCombiner
import com.emm.domain.shared.error.DomainException

class UpdateTransactionUseCase(
    private val repository: TransactionRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
) {

    suspend operator fun invoke(
        oldTransaction: Transaction,
        transactionUpdate: TransactionUpdate,
    ) {
        if (transactionUpdate.amount <= 0) {
            throw DomainException.ValidationError("El monto debe ser mayor a cero")
        }
        val dateAndTimeCombined: Long = dateAndTimeCombiner.combineWithUtc(transactionUpdate.date)
        val updatedTransaction: TransactionUpdate = transactionUpdate.copy(date = dateAndTimeCombined)
        repository.update(
            transactionId = oldTransaction.transactionId,
            transactionUpdate = updatedTransaction,
        )
    }
}
