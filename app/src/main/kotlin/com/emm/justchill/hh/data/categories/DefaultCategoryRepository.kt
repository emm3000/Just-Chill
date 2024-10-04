package com.emm.justchill.hh.data.categories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.justchill.Categories
import com.emm.justchill.CategoriesQueries
import com.emm.justchill.EmmDatabase
import com.emm.justchill.hh.data.shared.Syncer
import com.emm.justchill.hh.domain.categories.Category
import com.emm.justchill.hh.domain.categories.CategoryRepository
import com.emm.justchill.hh.domain.categories.CategoryUpsert
import com.emm.justchill.hh.domain.transaction.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultCategoryRepository(
    private val emmDatabase: EmmDatabase,
    private val syncer: Syncer,
) : CategoryRepository {

    private val cq: CategoriesQueries
        get() = emmDatabase.categoriesQueries

    override fun retrieve(): Flow<List<Category>> {
        return cq.retrieveAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map(List<Categories>::toDomain)
    }

    override fun findBy(categoryId: String): Flow<Category?> {
        return cq.find(categoryId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map {
                it?.let(Categories::toDomain)
            }
    }

    override suspend fun create(
        categoryId: String,
        categoryUpsert: CategoryUpsert,
    ) = withContext(Dispatchers.IO) {
        cq.insertCategory(
            categoryId = categoryId,
            name = categoryUpsert.name,
            type = categoryUpsert.type,
            description = categoryUpsert.description,
            syncStatus = SyncStatus.PENDING_INSERT.name
        )
//        syncer.sync(categoryId)
    }

    override suspend fun update(
        categoryId: String,
        categoryUpsert: CategoryUpsert,
    ) = withContext(Dispatchers.IO) {
        cq.updateValues(
            name = categoryUpsert.name,
            description = categoryUpsert.description,
            categoryId = categoryId,
            syncStatus = SyncStatus.PENDING_UPDATE.name
        )
//        syncer.sync(categoryId)
    }

    override suspend fun updateStatus(
        categoryId: String,
        syncStatus: SyncStatus,
    ) = withContext(Dispatchers.IO) {
        cq.updateStatus(
            syncStatus = syncStatus.name, categoryId = categoryId
        )
    }

    override suspend fun deleteBy(
        categoryId: String,
    ) = withContext(Dispatchers.IO) {
        cq.delete(categoryId)
    }
}