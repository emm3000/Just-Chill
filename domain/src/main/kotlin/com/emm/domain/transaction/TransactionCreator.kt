package com.emm.domain.transaction

import com.emm.domain.shared.DateAndTimeCombiner
import com.emm.domain.shared.UniqueIdProvider

class TransactionCreator(
    private val transactionRepository: TransactionRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
    private val uniqueIdProvider: UniqueIdProvider,
) {

    suspend fun create(transactionInsert: TransactionInsert) {
        val transactionId: String = uniqueIdProvider.id
        val dateAndTimeCombined: Long = dateAndTimeCombiner.combineWithUtc(transactionInsert.date)
        val transaction: TransactionInsert = transactionInsert.copy(
            id = transactionId,
            date = dateAndTimeCombined,
        )
        transactionRepository.create(transaction)
    }
}
