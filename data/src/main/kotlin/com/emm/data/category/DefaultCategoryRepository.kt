package com.emm.data.category

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.emm.data.Categories
import com.emm.data.CategoriesQueries
import com.emm.data.EmmDatabaseData
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryUpsert
import com.emm.domain.shared.UniqueIdProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultCategoryRepository(
    private val emmDatabase: EmmDatabaseData,
    private val uniqueIdProvider: UniqueIdProvider,
) : CategoryRepository {

    private val cq: CategoriesQueries
        get() = emmDatabase.categoriesQueries

    override fun retrieve(): Flow<List<Category>> = cq.all()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map(List<Categories>::toDomain)

    override fun findBy(categoryId: String): Flow<Category?> = cq.find(categoryId)
        .asFlow()
        .mapToOneOrNull(Dispatchers.IO)
        .map { category ->
            category?.let(Categories::toDomain)
        }

    override suspend fun create(categoryUpsert: CategoryUpsert) = withContext(Dispatchers.IO) {
        cq.insert(
            categoryId = uniqueIdProvider.id,
            name = categoryUpsert.name,
            type = categoryUpsert.type.name,
            description = categoryUpsert.description,
        )
    }

    override suspend fun update(categoryId: String, categoryUpsert: CategoryUpsert) = withContext(Dispatchers.IO) {
        cq.updateValues(
            name = categoryUpsert.name,
            description = categoryUpsert.description,
            categoryId = categoryId,
        )
    }

    override suspend fun deleteBy(categoryId: String) = withContext(Dispatchers.IO) {
        cq.delete(categoryId)
    }
}