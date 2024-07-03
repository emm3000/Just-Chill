package com.emm.justchill.hh.data.transactioncategory

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.justchill.TransactionsCategories
import com.emm.justchill.TransactionsCategoriesQueries
import com.emm.justchill.core.DispatchersProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TransactionCategoryDiskDataSource(
    dispatchersProvider: DispatchersProvider,
    private val queries: TransactionsCategoriesQueries,
) : com.emm.justchill.hh.data.transactioncategory.TransactionCategorySaver,
    com.emm.justchill.hh.data.transactioncategory.TransactionCategoryRetriever, DispatchersProvider by dispatchersProvider {

    override suspend fun save(
        transactionId: String,
        categoryId: String
    ) = withContext(ioDispatcher) {
        queries.add(transactionId, categoryId)
    }

    override fun retrieve(): Flow<List<TransactionsCategories>> {
        return queries.retrieve()
            .asFlow()
            .mapToList(ioDispatcher)
    }
}