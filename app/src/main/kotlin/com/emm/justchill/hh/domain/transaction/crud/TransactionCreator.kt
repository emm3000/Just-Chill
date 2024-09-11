package com.emm.justchill.hh.domain.transaction.crud

import com.emm.justchill.hh.domain.shared.UniqueIdProvider
import com.emm.justchill.hh.domain.shared.DateAndTimeCombiner
import com.emm.justchill.hh.domain.transaction.TransactionRepository
import com.emm.justchill.hh.domain.transaction.TransactionSyncer
import com.emm.justchill.hh.domain.transaction.model.TransactionInsert

class TransactionCreator(
    private val repository: TransactionRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
    private val transactionSyncer: TransactionSyncer,
    private val uniqueIdProvider: UniqueIdProvider,
) {

    suspend fun create(
        amount: String,
        transactionInsert: TransactionInsert,
    ) {

        val transactionId: String = uniqueIdProvider.uniqueId

        val dateAndTimeCombined: Long = dateAndTimeCombiner.combine(transactionInsert.date)

        val transaction: TransactionInsert = transactionInsert.copy(
            id = transactionId,
            amount = amount.toDouble(),
            date = dateAndTimeCombined
        )

        repository.add(transaction)

        transactionSyncer.sync(transactionId)
    }
}