package com.emm.justchill.hh.domain.transaction

import com.emm.justchill.hh.data.DefaultUniqueIdProvider
import com.emm.justchill.hh.data.UniqueIdProvider
import com.emm.justchill.hh.data.transaction.TransactionInsert
import com.emm.justchill.hh.domain.transactioncategory.AmountDbFormatter
import com.emm.justchill.hh.domain.transactioncategory.DateAndTimeCombiner

class TransactionAdder(
    private val repository: TransactionRepository,
    private val amountCleaner: AmountDbFormatter,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
    private val uniqueIdProvider: UniqueIdProvider = DefaultUniqueIdProvider,
) {

    suspend fun add(
        amount: String,
        transactionInsert: TransactionInsert
    ) {

        val amountFormated: Long = amountCleaner.format(amount)

        val transactionId: String = uniqueIdProvider.uniqueId

        val dateAndTimeCombined: Long = dateAndTimeCombiner.combine(transactionInsert.date)

        val transaction: TransactionInsert = transactionInsert.copy(
            id = transactionId,
            amount = amountFormated,
            date = dateAndTimeCombined
        )

        repository.add(transaction)
    }
}