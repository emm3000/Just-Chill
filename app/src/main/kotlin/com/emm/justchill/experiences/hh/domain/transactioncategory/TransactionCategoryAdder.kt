package com.emm.justchill.experiences.hh.domain.transactioncategory

import com.emm.justchill.experiences.hh.data.DefaultUniqueIdProvider
import com.emm.justchill.experiences.hh.data.transaction.TransactionInsert
import com.emm.justchill.experiences.hh.data.UniqueIdProvider
import com.emm.justchill.experiences.hh.domain.transaction.TransactionAdder

class TransactionCategoryAdder(
    private val transactionAdder: TransactionAdder,
    private val repository: TransactionCategoryRepository,
    private val uniqueIdProvider: UniqueIdProvider = DefaultUniqueIdProvider,
) {

    suspend fun add(categoryId: String, transactionInsert: TransactionInsert) {

        val transactionId: String = uniqueIdProvider.uniqueId
        val transaction: TransactionInsert = transactionInsert.copy(id = transactionId)

        transactionAdder.add(transaction)
        repository.add(transactionId = transactionId, categoryId = categoryId)
    }
}