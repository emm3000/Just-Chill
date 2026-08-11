package com.emm.domain.transaction

import com.emm.domain.shared.DateAndTimeCombiner
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode

class UpdateTransactionUseCase(
    private val repository: TransactionRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
) {

    suspend operator fun invoke(oldTransaction: Transaction, transactionUpdate: TransactionUpdate) {
        if (transactionUpdate.amount.cents <= 0) {
            throw DomainException.ValidationError(
                "Amount must be greater than zero",
                ValidationCode.AmountMustBePositive,
            )
        }
        val dateAndTimeCombined: Long = dateAndTimeCombiner.combineKeepingTimeOf(
            dateInMillis = transactionUpdate.date,
            timeSourceInMillis = oldTransaction.date,
        )
        val updatedTransaction: TransactionUpdate = transactionUpdate.copy(date = dateAndTimeCombined)
        repository.update(
            transactionId = oldTransaction.transactionId,
            transactionUpdate = updatedTransaction,
        )
    }
}
