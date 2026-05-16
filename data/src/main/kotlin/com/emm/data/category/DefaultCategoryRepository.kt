package com.emm.data.category

import com.emm.data.shared.catchAsDomainException
import com.emm.data.shared.safeApiCall
import com.emm.data.shared.safeDbCall
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryUpsert
import com.emm.domain.shared.SyncState
import kotlinx.coroutines.flow.Flow

class DefaultCategoryRepository(
    private val localDataSource: CategoryLocalDataSource,
    private val remoteDataSource: CategoryRemoteDataSource,
) : CategoryRepository {

    override fun all(): Flow<List<Category>> = localDataSource.all().catchAsDomainException()

    override fun find(categoryId: String): Flow<Category?> = localDataSource.find(categoryId).catchAsDomainException()

    override suspend fun create(categoryUpsert: CategoryUpsert): Unit = safeDbCall {
        localDataSource.create(categoryUpsert)
        Unit
    }

    override suspend fun update(categoryId: String, categoryUpsert: CategoryUpsert): Unit = safeDbCall {
        localDataSource.update(categoryId, categoryUpsert)
        Unit
    }

    override suspend fun delete(categoryId: String): Unit = safeDbCall {
        localDataSource.softDelete(categoryId)
        Unit
    }

    override suspend fun count(): Long = safeDbCall {
        localDataSource.countDefaults()
    }

    override suspend fun sync(): Unit = safeApiCall {
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
        Unit
    }

    override suspend fun pull() = safeApiCall {
        val networkCategories: List<NetworkCategory> = remoteDataSource.all()
        networkCategories.forEach { network ->
            val localEntity = localDataSource.findEntity(network.categoryId)
            when {
                localEntity == null -> {
                    // New from remote: insert with placeholder icon/color/type (remote schema limitation)
                    localDataSource.insertSynced(network.asEntity())
                }
                localEntity.syncState == SyncState.Pending.name -> Unit // local wins; sync() will push it
                network.updatedAt > localEntity.updatedAt -> {
                    // Remote name is newer; preserve local icon/color/type
                    localDataSource.updateNameFromRemote(network.categoryId, network.name, network.updatedAt)
                }
                // else remote is same age or older and local is synced: nothing to do
            }
        }
    }

    private suspend fun updatedRemoteCategories(unSynced: List<CategoryEntity>) {
        val networkCategories = unSynced.map { it.asNetworkModel(userId = "") }
        remoteDataSource.upsert(networkCategories)
    }
}
