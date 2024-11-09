package com.emm.justchill.hh.category.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.justchill.Categories
import com.emm.justchill.CategoriesQueries
import com.emm.justchill.EmmDatabase
import com.emm.justchill.hh.shared.Syncer
import com.emm.justchill.hh.category.domain.Category
import com.emm.justchill.hh.category.domain.CategoryRepository
import com.emm.justchill.hh.category.domain.CategoryUpsert
import com.emm.justchill.hh.transaction.domain.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultCategoryRepository(
    private val emmDatabase: EmmDatabase,
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
        )
    }

    override suspend fun update(
        categoryId: String,
        categoryUpsert: CategoryUpsert,
    ) = withContext(Dispatchers.IO) {
        cq.updateValues(
            name = categoryUpsert.name,
            description = categoryUpsert.description,
            categoryId = categoryId,
        )
    }

    override suspend fun deleteBy(
        categoryId: String,
    ) = withContext(Dispatchers.IO) {
        cq.delete(categoryId)
    }
}