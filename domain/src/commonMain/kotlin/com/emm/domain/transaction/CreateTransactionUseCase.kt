package com.emm.domain.transaction

import com.emm.domain.shared.DateAndTimeCombiner
import com.emm.domain.shared.TransactionId
import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode

class CreateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
    private val uniqueIdProvider: UniqueIdProvider,
) {

    suspend operator fun invoke(transactionInsert: TransactionInsert) {
        if (transactionInsert.amount.cents <= 0) {
            throw DomainException.ValidationError(
                "Amount must be greater than zero",
                ValidationCode.AmountMustBePositive,
            )
        }
        val dateAndTimeCombined: Long = dateAndTimeCombiner.combineWithCurrentTime(transactionInsert.date)
        val transaction: TransactionInsert = transactionInsert.copy(
            id = TransactionId(uniqueIdProvider.id),
            date = dateAndTimeCombined,
        )
        transactionRepository.create(transaction)
    }
}
