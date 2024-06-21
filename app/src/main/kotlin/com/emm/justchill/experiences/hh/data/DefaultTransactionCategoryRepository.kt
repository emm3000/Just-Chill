package com.emm.justchill.experiences.hh.data

import com.emm.justchill.experiences.hh.domain.TransactionCategoryRepository

class DefaultTransactionCategoryRepository(
    private val saver: TransactionCategorySaver,
) : TransactionCategoryRepository {

    override suspend fun add(transactionId: String, categoryId: String) {
        saver.save(transactionId, categoryId)
    }
}