package com.emm.justchill.experiences.hh.domain

import com.emm.justchill.experiences.hh.data.DefaultUniqueIdProvider
import com.emm.justchill.experiences.hh.data.TransactionInsert
import com.emm.justchill.experiences.hh.data.UniqueIdProvider

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