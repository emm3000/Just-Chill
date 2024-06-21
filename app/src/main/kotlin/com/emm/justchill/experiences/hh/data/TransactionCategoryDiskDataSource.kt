package com.emm.justchill.experiences.hh.data

import com.emm.justchill.TransactionsCategoriesQueries
import com.emm.justchill.core.DispatchersProvider
import kotlinx.coroutines.withContext

class TransactionCategoryDiskDataSource(
    dispatchersProvider: DispatchersProvider,
    private val queries: TransactionsCategoriesQueries,
) : TransactionCategorySaver, DispatchersProvider by dispatchersProvider {

    override suspend fun save(
        transactionId: String,
        categoryId: String
    ) = withContext(ioDispatcher) {
        queries.add(transactionId, categoryId)
    }
}