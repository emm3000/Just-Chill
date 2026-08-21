package com.emm.domain.transaction

import com.emm.domain.shared.ensureNotFutureDated
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class UpdateTransactionUseCase(
    private val repository: TransactionRepository,
    private val clock: Clock,
    private val zone: TimeZone,
) {

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
