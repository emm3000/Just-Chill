package com.emm.justchill.hh.data.category

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.justchill.Categories
import com.emm.justchill.CategoriesQueries
import com.emm.justchill.core.DispatchersProvider
import com.emm.justchill.hh.data.DefaultNowProvider
import com.emm.justchill.hh.data.DefaultUniqueIdProvider
import com.emm.justchill.hh.data.NowProvider
import com.emm.justchill.hh.data.UniqueIdProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CategoryDiskDataSource(
    dispatchersProvider: DispatchersProvider,
    private val categoryQueries: CategoriesQueries,
    private val nowProvider: com.emm.justchill.hh.data.NowProvider = com.emm.justchill.hh.data.DefaultNowProvider,
    private val idProvider: com.emm.justchill.hh.data.UniqueIdProvider = com.emm.justchill.hh.data.DefaultUniqueIdProvider,
) : com.emm.justchill.hh.data.category.CategorySaver,
    com.emm.justchill.hh.data.category.AllCategoriesRetriever, DispatchersProvider by dispatchersProvider {

    override suspend fun save(name: String, type: String) = withContext(ioDispatcher) {
        categoryQueries.addCategory(
            categoryId = idProvider.uniqueId,
            name = name,
            type = type,
            createdAt = nowProvider.now,
            updatedAt = nowProvider.now
        )
    }

    override fun retrieve(): Flow<List<Categories>> {
        return categoryQueries
            .retrieveAll()
            .asFlow()
            .mapToList(ioDispatcher)
    }
}