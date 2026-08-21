package com.emm.domain.transaction

import com.emm.domain.shared.ensureNotFutureDated
import com.emm.domain.shared.ensurePositiveAmount
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class UpdateTransactionUseCase(
    private val repository: TransactionRepository,
    private val clock: Clock,
    private val zone: TimeZone,
) {

    suspend operator fun invoke(oldTransaction: Transaction, transactionUpdate: TransactionUpdate) {
        ensurePositiveAmount(transactionUpdate.amount)
        ensureNotFutureDated(transactionUpdate.occurredAt, clock, zone)
        repository.update(
            transactionId = oldTransaction.transactionId,
            transactionUpdate = transactionUpdate,
        )
    }
}
