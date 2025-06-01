package com.emm.domain.category

import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun all(): Flow<List<Category>>

    fun find(categoryId: String): Flow<Category?>

    suspend fun create(categoryUpsert: CategoryUpsert)

    suspend fun update(categoryId: String, categoryUpsert: CategoryUpsert)

    suspend fun delete(categoryId: String)
}