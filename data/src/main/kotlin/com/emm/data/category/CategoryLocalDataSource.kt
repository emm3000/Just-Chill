package com.emm.data.category

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.CategoriesQueries
import com.emm.data.EmmDatabaseData
import com.emm.data.shared.ioDispatcher
import com.emm.data.shared.nowMillis
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryUpsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class CategoryLocalDataSource(private val emmDatabase: EmmDatabaseData, private val clock: Clock) {

    private val cq: CategoriesQueries
        get() = emmDatabase.categoriesQueries

    fun all(): Flow<List<Category>> = cq.all()
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list -> list.asEntity().asExternalModel() }

    suspend fun countDefaults(): Long = withContext(ioDispatcher) {
        cq.countDefaultCategories().executeAsOne()
    }

    suspend fun create(categoryUpsert: CategoryUpsert) = withContext(ioDispatcher) {
        val now = clock.nowMillis()
        cq.insert(
            categoryId = categoryUpsert.categoryId.value,
            name = categoryUpsert.name,
            icon = categoryUpsert.icon,
            color = categoryUpsert.color,
            categoryType = categoryUpsert.categoryType.name,
            isDefault = false,
            createdAt = now,
            updatedAt = now,
        )
    }

    suspend fun update(categoryId: String, categoryUpsert: CategoryUpsert) = withContext(ioDispatcher) {
        cq.updateValues(
            name = categoryUpsert.name,
            categoryId = categoryId,
            updatedAt = clock.nowMillis(),
        )
    }

    suspend fun softDelete(categoryId: String) = withContext(ioDispatcher) {
        val now = clock.nowMillis()
        cq.softDelete(deletedAt = now, updatedAt = now, categoryId = categoryId)
    }
}
