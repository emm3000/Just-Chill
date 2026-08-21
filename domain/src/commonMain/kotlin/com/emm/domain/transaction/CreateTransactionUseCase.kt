package com.emm.domain.transaction

import com.emm.domain.shared.TransactionId
import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.shared.ensureNotFutureDated
import com.emm.domain.shared.ensurePositiveAmount
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class CreateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val uniqueIdProvider: UniqueIdProvider,
    private val clock: Clock,
    private val zone: TimeZone,
) {

    suspend operator fun invoke(transactionInsert: TransactionInsert) {
        ensurePositiveAmount(transactionInsert.amount)
        ensureNotFutureDated(transactionInsert.occurredAt, clock, zone)
        val transaction: TransactionInsert = transactionInsert.copy(id = TransactionId(uniqueIdProvider.id))
        transactionRepository.create(transaction)
    }
}
