package com.emm.justchill.experiences.hh.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.justchill.Categories
import com.emm.justchill.CategoriesQueries
import com.emm.justchill.core.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CategoryDiskDataSource(
    dispatchers: Dispatchers,
    private val categoryQueries: CategoriesQueries,
    private val nowProvider: NowProvider = DefaultNowProvider,
) : CategorySaver, AllItemsRetriever<Categories>, Dispatchers by dispatchers {

    override suspend fun save(name: String, type: String) = withContext(ioDispatcher) {
        categoryQueries.addCategory(
            categoryId = null,
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