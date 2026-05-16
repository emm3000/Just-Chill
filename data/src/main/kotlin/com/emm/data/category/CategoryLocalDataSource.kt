package com.emm.data.category

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.data.CategoriesQueries
import com.emm.data.EmmDatabaseData
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryUpsert
import com.emm.domain.shared.SyncState
import com.emm.domain.shared.currentTimeInMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CategoryLocalDataSource(private val emmDatabase: EmmDatabaseData) {

    private val cq: CategoriesQueries
        get() = emmDatabase.categoriesQueries

    fun all(): Flow<List<Category>> = cq.all()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { list -> list.asEntity().asExternalModel() }

    fun find(categoryId: String): Flow<Category?> = cq.find(categoryId)
        .asFlow()
        .mapToOneOrNull(Dispatchers.IO)
        .map { category ->
            category?.asEntity()?.asExternalModel()
        }

    suspend fun countDefaults(): Long = withContext(Dispatchers.IO) {
        cq.countDefaultCategories().executeAsOne()
    }

    suspend fun create(categoryUpsert: CategoryUpsert) = withContext(Dispatchers.IO) {
        cq.insert(
            categoryId = categoryUpsert.categoryId,
            name = categoryUpsert.name,
            icon = categoryUpsert.icon,
            color = categoryUpsert.color,
            categoryType = categoryUpsert.categoryType.name,
            syncState = SyncState.Pending.name,
            isDeleted = false,
            isDefault = false,
            createdAt = currentTimeInMillis(),
            updatedAt = currentTimeInMillis(),
        )
    }

    suspend fun update(categoryId: String, categoryUpsert: CategoryUpsert) = withContext(Dispatchers.IO) {
        cq.updateValues(
            name = categoryUpsert.name,
            categoryId = categoryId,
            syncState = SyncState.Pending.name,
            updatedAt = currentTimeInMillis(),
        )
    }

    suspend fun softDelete(categoryId: String) = withContext(Dispatchers.IO) {
        cq.softDelete(currentTimeInMillis(), categoryId)
    }

    suspend fun unSynced(): List<CategoryEntity> = withContext(Dispatchers.IO) {
        cq.selectPendingSync().executeAsList().asEntity()
    }

    suspend fun findEntity(categoryId: String): CategoryEntity? = withContext(Dispatchers.IO) {
        cq.find(categoryId).executeAsOneOrNull()?.asEntity()
    }

    suspend fun insertSynced(entity: CategoryEntity) = withContext(Dispatchers.IO) {
        cq.insert(
            categoryId = entity.categoryId,
            name = entity.name,
            icon = entity.icon,
            color = entity.color,
            categoryType = entity.categoryType,
            syncState = SyncState.Synced.name,
            isDefault = entity.isDefault,
            isDeleted = entity.isDeleted,
            updatedAt = entity.updatedAt,
            createdAt = entity.createdAt,
        )
    }

    suspend fun updateNameFromRemote(categoryId: String, name: String, updatedAt: Long) = withContext(Dispatchers.IO) {
        cq.updateValues(
            name = name,
            syncState = SyncState.Synced.name,
            updatedAt = updatedAt,
            categoryId = categoryId,
        )
    }
}
