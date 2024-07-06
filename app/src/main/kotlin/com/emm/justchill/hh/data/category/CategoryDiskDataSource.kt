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
import com.emm.justchill.hh.domain.CategoryModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CategoryDiskDataSource(
    dispatchersProvider: DispatchersProvider,
    private val categoryQueries: CategoriesQueries,
    private val nowProvider: NowProvider = DefaultNowProvider,
    private val idProvider: UniqueIdProvider = DefaultUniqueIdProvider,
) : CategorySaver,
    AllCategoriesRetriever,
    CategorySeeder,
    DispatchersProvider by dispatchersProvider
{

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

    override suspend fun seed(data: List<CategoryModel>) = withContext(ioDispatcher) {
        categoryQueries.transaction {
            populateCategoryTable(data)
        }
    }

    private fun populateCategoryTable(data: List<CategoryModel>) {
        data.forEach { category ->
            categoryQueries.addCategory(
                categoryId = category.categoryId,
                name = category.name,
                type = category.type,
                createdAt = category.createdAt,
                updatedAt = category.updatedAt
            )
        }
    }
}