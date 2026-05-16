package com.emm.domain.category

import com.emm.domain.shared.CategoryId
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun all(): Flow<List<Category>>

    fun find(categoryId: CategoryId): Flow<Category?>

    suspend fun create(categoryUpsert: CategoryUpsert)

    suspend fun count(): Long

    suspend fun update(categoryId: CategoryId, categoryUpsert: CategoryUpsert)

    suspend fun delete(categoryId: CategoryId)

    suspend fun sync()

    suspend fun pull()
}