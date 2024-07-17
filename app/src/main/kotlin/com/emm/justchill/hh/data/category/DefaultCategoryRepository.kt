package com.emm.justchill.hh.data.category

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.justchill.Categories
import com.emm.justchill.CategoriesQueries
import com.emm.justchill.core.DispatchersProvider
import com.emm.justchill.hh.data.NowProvider
import com.emm.justchill.hh.data.UniqueIdProvider
import com.emm.justchill.hh.domain.CategoryModel
import com.emm.justchill.hh.domain.AndroidDataProvider
import com.emm.justchill.hh.domain.auth.AuthRepository
import com.emm.justchill.hh.domain.category.CategoryRepository
import com.emm.justchill.hh.domain.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DefaultCategoryRepository(
    dispatchersProvider: DispatchersProvider,
    private val categoriesQueries: CategoriesQueries,
    private val categoryNetworkDataSource: CategoryNetworkDataSource,
    private val nowProvider: NowProvider,
    private val uniqueIdProvider: UniqueIdProvider,
    private val androidDataProvider: AndroidDataProvider,
    private val authRepository: AuthRepository,
) : CategoryRepository, DispatchersProvider by dispatchersProvider {

    override suspend fun add(name: String, type: String) = withContext(ioDispatcher) {
        categoriesQueries.addCategory(
            categoryId = uniqueIdProvider.uniqueId,
            name = name,
            type = type,
            createdAt = nowProvider.now,
            updatedAt = nowProvider.now
        )
    }

    override fun all(): Flow<List<Categories>> {
        return categoriesQueries
            .retrieveAll()
            .asFlow()
            .mapToList(ioDispatcher)
    }

    override suspend fun seed() {
        val networkCategories: List<CategoryModel> = categoryNetworkDataSource.retrieve()
        categoriesQueries.transaction {
            populate(networkCategories)
        }
    }

    private fun populate(data: List<CategoryModel>) {
        data.forEach { categoryModel ->
            categoriesQueries.addCategory(
                categoryId = categoryModel.categoryId,
                name = categoryModel.name,
                type = categoryModel.type,
                createdAt = categoryModel.createdAt,
                updatedAt = categoryModel.updatedAt
            )
        }
    }

    override suspend fun backup() {
        val authId: String = authRepository.session()?.id ?: return
        val categories: List<CategoryModel> = categoriesQueries
            .retrieveAll()
            .executeAsList()
            .map(Categories::toModel)
            .map {
                it.copy(
                    deviceId = androidDataProvider.androidId(),
                    deviceName = androidDataProvider.deviceName(),
                    userId = authId
                )
            }

        categoryNetworkDataSource.upsert(categories)
    }
}