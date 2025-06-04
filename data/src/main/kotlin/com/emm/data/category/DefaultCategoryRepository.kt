package com.emm.data.category

import com.emm.data.Categories
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryUpsert
import kotlinx.coroutines.flow.Flow

class DefaultCategoryRepository(
    private val localDataSource: CategoryLocalDataSource,
    private val remoteDataSource: CategoryRemoteDataSource,
) : CategoryRepository {

    override fun all(): Flow<List<Category>> = localDataSource.all()

    override fun find(categoryId: String): Flow<Category?> = localDataSource.find(categoryId)

    override suspend fun create(categoryUpsert: CategoryUpsert) {
        localDataSource.create(categoryUpsert)
    }

    override suspend fun update(categoryId: String, categoryUpsert: CategoryUpsert) {
        localDataSource.update(categoryId, categoryUpsert)
    }

    override suspend fun delete(categoryId: String) {
        localDataSource.delete(categoryId)
    }

    override suspend fun sync() {
        val unSynced: List<Categories> = localDataSource.unSynced()
        updatedRemoteCategories(unSynced)

        val syncedCategories: List<CategoryUpsert> = unSynced.map(Categories::toCategoryUpsert)

        unSynced.zip(syncedCategories) { category, categoryUpsert ->
            localDataSource.update(category.categoryId, categoryUpsert)
        }
    }

    override suspend fun pull() {
        val categoryModelsFromRemote: List<CategoryModel> = remoteDataSource.all()
        val categoryUpsertList: List<CategoryUpsert> = categoryModelsFromRemote.map(CategoryModel::toCategoryUpsert)
        categoryUpsertList.forEach {
            localDataSource.create(it)
        }
    }

    private suspend fun updatedRemoteCategories(unSynced: List<Categories>) {
        val categoryModels = unSynced.map(Categories::toCategoryModel)
        remoteDataSource.upsert(categoryModels)
    }
}