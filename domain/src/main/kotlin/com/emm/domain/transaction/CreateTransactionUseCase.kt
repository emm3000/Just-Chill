package com.emm.domain.transaction

import com.emm.domain.shared.DateAndTimeCombiner
import com.emm.domain.shared.TransactionId
import com.emm.domain.shared.UniqueIdProvider

class CreateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
    private val uniqueIdProvider: UniqueIdProvider,
) {

    suspend operator fun invoke(transactionInsert: TransactionInsert) {
        val dateAndTimeCombined: Long = dateAndTimeCombiner.combineWithUtc(transactionInsert.date)
        val transaction: TransactionInsert = transactionInsert.copy(
            id = TransactionId(uniqueIdProvider.id),
            date = dateAndTimeCombined,
        )
        transactionRepository.create(transaction)
    }
}
