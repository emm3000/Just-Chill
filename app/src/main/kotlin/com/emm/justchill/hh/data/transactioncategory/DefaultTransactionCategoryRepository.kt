package com.emm.justchill.hh.data.transactioncategory

import com.emm.justchill.TransactionsCategories
import com.emm.justchill.hh.domain.TransactionCategoryModel
import com.emm.justchill.hh.domain.transactioncategory.TransactionCategoryRepository
import kotlinx.coroutines.flow.Flow

class DefaultTransactionCategoryRepository(
    private val saver: TransactionCategorySaver,
    private val retriever: TransactionCategoryRetriever,
    private val transactionCategorySeeder: TransactionCategorySeeder,
) : TransactionCategoryRepository {

    override suspend fun add(transactionId: String, categoryId: String) {
        saver.save(transactionId, categoryId)
    }

    override fun retrieve(): Flow<List<TransactionsCategories>> {
        return retriever.retrieve()
    }

    override suspend fun seed(data: List<TransactionCategoryModel>) {
        transactionCategorySeeder.seed(data)
    }
}