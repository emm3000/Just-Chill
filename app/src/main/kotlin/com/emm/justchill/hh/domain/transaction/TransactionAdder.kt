package com.emm.justchill.hh.domain.transaction

import com.emm.justchill.hh.data.DefaultUniqueIdProvider
import com.emm.justchill.hh.data.UniqueIdProvider
import com.emm.justchill.hh.data.transaction.TransactionInsert
import com.emm.justchill.hh.domain.DateAndTimeCombiner
import com.emm.justchill.hh.domain.transaction.remote.TransactionAdderResolver

class TransactionAdder(
    private val repository: TransactionRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
    private val transactionAdderResolver: TransactionAdderResolver,
    private val uniqueIdProvider: UniqueIdProvider = DefaultUniqueIdProvider,
) {

    suspend fun add(
        amount: String,
        transactionInsert: TransactionInsert
    ) {

        val transactionId: String = uniqueIdProvider.uniqueId

        val dateAndTimeCombined: Long = dateAndTimeCombiner.combine(transactionInsert.date)

        val transaction: TransactionInsert = transactionInsert.copy(
            id = transactionId,
            amount = amount.toDouble(),
            date = dateAndTimeCombined
        )

        repository.add(transaction)

        transactionAdderResolver.resolve(transactionId)
    }
}