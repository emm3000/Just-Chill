package com.emm.data.category

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.data.Categories
import com.emm.data.CategoriesQueries
import com.emm.data.EmmDatabaseData
import com.emm.data.currentTimeInMillis
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryUpsert
import com.emm.domain.shared.UniqueIdProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CategoryLocalDataSource(
    private val emmDatabase: EmmDatabaseData,
    private val uniqueIdProvider: UniqueIdProvider,
) {

    private val cq: CategoriesQueries
        get() = emmDatabase.categoriesQueries

    fun all(): Flow<List<Category>> = cq.all()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map(List<Categories>::toDomain)

    fun find(categoryId: String): Flow<Category?> = cq.find(categoryId)
        .asFlow()
        .mapToOneOrNull(Dispatchers.IO)
        .map { category ->
            category?.let(Categories::toDomain)
        }

    suspend fun create(categoryUpsert: CategoryUpsert) = withContext(Dispatchers.IO) {
        cq.insert(
            categoryId = uniqueIdProvider.id,
            name = categoryUpsert.name,
            updatedAt = currentTimeInMillis()
        )
    }

    suspend fun update(categoryId: String, categoryUpsert: CategoryUpsert) = withContext(Dispatchers.IO) {
        cq.updateValues(
            name = categoryUpsert.name,
            categoryId = categoryId,
            updatedAt = currentTimeInMillis()
        )
    }

    suspend fun delete(categoryId: String) = withContext(Dispatchers.IO) {
        cq.delete(categoryId)
    }
}