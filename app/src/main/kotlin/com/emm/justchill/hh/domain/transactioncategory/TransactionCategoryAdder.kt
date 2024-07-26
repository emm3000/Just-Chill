package com.emm.justchill.hh.domain.transactioncategory

import com.emm.justchill.hh.data.DefaultUniqueIdProvider
import com.emm.justchill.hh.data.UniqueIdProvider
import com.emm.justchill.hh.data.transaction.TransactionInsert
import com.emm.justchill.hh.domain.transaction.TransactionAdder

class TransactionCategoryAdder(
    private val transactionAdder: TransactionAdder,
    private val repository: TransactionCategoryRepository,
    private val amountCleaner: AmountDbFormatter,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
    private val uniqueIdProvider: UniqueIdProvider = DefaultUniqueIdProvider,
) {

    suspend fun add(
        categoryId: String,
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

        transactionAdder.add(amount, transaction)
        repository.add(transactionId = transactionId, categoryId = categoryId)
    }
}