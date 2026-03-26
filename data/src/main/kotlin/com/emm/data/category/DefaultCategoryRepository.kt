package com.emm.data.category

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
        localDataSource.softDelete(categoryId)
    }

    override suspend fun count(): Long = localDataSource.countDefaults()

    override suspend fun sync() {
        val unSynced: List<CategoryEntity> = localDataSource.unSynced()
        updatedRemoteCategories(unSynced)

        val syncedCategoryUpserts: List<CategoryUpsert> = unSynced.map { entity ->
            CategoryUpsert(
                categoryId = entity.categoryId,
                name = entity.name,
                icon = entity.icon,
                color = entity.color,
                categoryType = enumValueOf(entity.categoryType),
            )
        }

        unSynced.zip(syncedCategoryUpserts) { entity, categoryUpsert ->
            localDataSource.update(entity.categoryId, categoryUpsert)
        }
    }

    override suspend fun pull() {
        val networkCategories: List<NetworkCategory> = remoteDataSource.all()
        val categoryUpsertList: List<CategoryUpsert> = networkCategories.map { network ->
            CategoryUpsert(
                categoryId = network.categoryId,
                name = network.name,
                icon = "icon",
                color = "color",
                categoryType = enumValueOf("Income"),
            )
        }
        categoryUpsertList.forEach {
            localDataSource.create(it)
        }
    }

    private suspend fun updatedRemoteCategories(unSynced: List<CategoryEntity>) {
        val networkCategories = unSynced.map { it.asNetworkModel(userId = "") }
        remoteDataSource.upsert(networkCategories)
    }
}
