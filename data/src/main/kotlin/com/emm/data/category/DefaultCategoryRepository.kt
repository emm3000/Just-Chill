package com.emm.data.category

import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryUpsert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DefaultCategoryRepository(
    private val localDataSource: LocalCategoryDataSource,
) : CategoryRepository {

    override fun all(): Flow<List<Category>> = localDataSource.all()

    override fun find(categoryId: String): Flow<Category?> = localDataSource.find(categoryId)

    override suspend fun create(categoryUpsert: CategoryUpsert) = withContext(Dispatchers.IO) {
        localDataSource.create(categoryUpsert)
    }

    override suspend fun update(categoryId: String, categoryUpsert: CategoryUpsert) = withContext(Dispatchers.IO) {
        localDataSource.update(categoryId, categoryUpsert)
    }

    override suspend fun delete(categoryId: String) = withContext(Dispatchers.IO) {
        localDataSource.delete(categoryId)
    }
}