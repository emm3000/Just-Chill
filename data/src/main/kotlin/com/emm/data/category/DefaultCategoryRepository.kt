package com.emm.data.category

import com.emm.data.Categories
import com.emm.data.sync.Synchronizer
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryUpsert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DefaultCategoryRepository(
    private val localDataSource: CategoryLocalDataSource,
    private val remoteDataSource: RemoteCategoryDataSource,
) : CategoryRepository, Synchronizer {

    override fun all(): Flow<List<Category>> = localDataSource.all()

    override fun find(categoryId: String): Flow<Category?> = localDataSource.find(categoryId)

    override suspend fun create(categoryUpsert: CategoryUpsert) {
        localDataSource.create(categoryUpsert)
    }

    override suspend fun update(categoryId: String, categoryUpsert: CategoryUpsert) {
        localDataSource.update(categoryId, categoryUpsert)
    }

    override suspend fun delete(categoryId: String) = withContext(Dispatchers.IO) {
        localDataSource.delete(categoryId)
    }

    override suspend fun sync() {
        val unSynced: List<Categories> = localDataSource.unSynced()
        updatedRemoteCategories(unSynced)

        val syncedCategories: List<CategoryUpsert> = unSynced.map(::toCategoryUpsert)

        unSynced.zip(syncedCategories) { category, categoryUpsert ->
            localDataSource.update(category.categoryId, categoryUpsert)
        }
    }

    private suspend fun updatedRemoteCategories(unSynced: List<Categories>) {
        val categoryModels = unSynced.map(::toCategoryModel)
        remoteDataSource.upsert(categoryModels)
    }

    private fun toCategoryUpsert(categories: Categories) = CategoryUpsert(
        name = categories.name,
        isSynced = true,
    )

    private fun toCategoryModel(category: Categories) = CategoryModel(
        categoryId = category.categoryId,
        name = category.name,
        updatedAt = category.updatedAt,
    )
}