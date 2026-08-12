package com.emm.domain.transaction

import com.emm.domain.shared.TransactionId
import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class CreateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val uniqueIdProvider: UniqueIdProvider,
    private val clock: Clock,
    private val zone: TimeZone,
) {

    /**
     * [transactionInsert]`.occurredAt` is stored exactly as given. Nothing here derives, combines
     * or re-resolves it — that derivation is what the two representations of "when" used to need,
     * and it is what kept breaking.
     */
    suspend operator fun invoke(transactionInsert: TransactionInsert) {
        if (transactionInsert.amount.cents <= 0) {
            throw DomainException.ValidationError(
                "Amount must be greater than zero",
                ValidationCode.AmountMustBePositive,
            )
        }
        ensureNotFutureDated(transactionInsert.occurredAt, clock, zone)
        val transaction: TransactionInsert = transactionInsert.copy(id = TransactionId(uniqueIdProvider.id))
        transactionRepository.create(transaction)
    }
}
