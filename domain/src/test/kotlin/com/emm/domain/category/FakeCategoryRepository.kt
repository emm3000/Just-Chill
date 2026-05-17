package com.emm.domain.category

import com.emm.domain.shared.CategoryId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeCategoryRepository : CategoryRepository {

    var lastCreated: CategoryUpsert? = null
    var lastUpdatedId: CategoryId? = null
    var lastUpdate: CategoryUpsert? = null
    var lastDeleted: CategoryId? = null
    var categoryToReturn: Category? = null
    var countToReturn: Long = 0L

    override fun all(): Flow<List<Category>> = flowOf(emptyList())

    override suspend fun find(categoryId: CategoryId): Category? = categoryToReturn

    override suspend fun create(categoryUpsert: CategoryUpsert) {
        lastCreated = categoryUpsert
    }

    override suspend fun count(): Long = countToReturn

    override suspend fun update(categoryId: CategoryId, categoryUpsert: CategoryUpsert) {
        lastUpdatedId = categoryId
        lastUpdate = categoryUpsert
    }

    override suspend fun delete(categoryId: CategoryId) {
        lastDeleted = categoryId
    }
}
