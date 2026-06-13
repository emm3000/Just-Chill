package com.emm.data.category

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.data.CategoriesQueries
import com.emm.data.EmmDatabaseData
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryUpsert
import com.emm.domain.shared.currentTimeInMillis
import com.emm.data.shared.ioDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CategoryLocalDataSource(private val emmDatabase: EmmDatabaseData) {

    private val cq: CategoriesQueries
        get() = emmDatabase.categoriesQueries

    fun all(): Flow<List<Category>> = cq.all()
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list -> list.asEntity().asExternalModel() }

    fun find(categoryId: String): Flow<Category?> = cq.find(categoryId)
        .asFlow()
        .mapToOneOrNull(ioDispatcher)
        .map { category ->
            category?.asEntity()?.asExternalModelOrNull()
        }

    suspend fun countDefaults(): Long = withContext(ioDispatcher) {
        cq.countDefaultCategories().executeAsOne()
    }

    suspend fun create(categoryUpsert: CategoryUpsert) = withContext(ioDispatcher) {
        cq.insert(
            categoryId = categoryUpsert.categoryId.value,
            name = categoryUpsert.name,
            icon = categoryUpsert.icon,
            color = categoryUpsert.color,
            categoryType = categoryUpsert.categoryType.name,
            isDefault = false,
            createdAt = currentTimeInMillis(),
            updatedAt = currentTimeInMillis(),
        )
    }

    suspend fun update(categoryId: String, categoryUpsert: CategoryUpsert) = withContext(ioDispatcher) {
        cq.updateValues(
            name = categoryUpsert.name,
            categoryId = categoryId,
            updatedAt = currentTimeInMillis(),
        )
    }

    suspend fun softDelete(categoryId: String) = withContext(ioDispatcher) {
        val now = currentTimeInMillis()
        cq.softDelete(deletedAt = now, updatedAt = now, categoryId = categoryId)
    }
}
