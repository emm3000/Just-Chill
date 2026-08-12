package com.emm.domain.transaction

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class UpdateTransactionUseCase(
    private val repository: TransactionRepository,
    private val clock: Clock,
    private val zone: TimeZone,
) {

    /**
     * [transactionUpdate]`.occurredAt` is stored exactly as given, the same as on the create path.
     *
     * This is where the corruption used to live. The value had to be taken apart into a day and an
     * instant and put back together against a timezone, and the two halves stopped being inverses
     * of each other. There is nothing left to take apart: an edit that did not touch the date
     * writes back the value it loaded, unchanged.
     */
    suspend operator fun invoke(oldTransaction: Transaction, transactionUpdate: TransactionUpdate) {
        if (transactionUpdate.amount.cents <= 0) {
            throw DomainException.ValidationError(
                "Amount must be greater than zero",
                ValidationCode.AmountMustBePositive,
            )
        }
        ensureNotFutureDated(transactionUpdate.occurredAt, clock, zone)
        repository.update(
            transactionId = oldTransaction.transactionId,
            transactionUpdate = transactionUpdate,
        )
    }
}
