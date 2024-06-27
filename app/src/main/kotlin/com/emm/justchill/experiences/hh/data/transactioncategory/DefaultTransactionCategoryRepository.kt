package com.emm.justchill.experiences.hh.data.transactioncategory

import com.emm.justchill.experiences.hh.domain.transactioncategory.TransactionCategoryRepository

class DefaultTransactionCategoryRepository(
    private val saver: TransactionCategorySaver,
) : TransactionCategoryRepository {

    override suspend fun add(transactionId: String, categoryId: String) {
        saver.save(transactionId, categoryId)
    }
}