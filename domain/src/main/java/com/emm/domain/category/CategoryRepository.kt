package com.emm.domain.category

import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun retrieve(): Flow<List<Category>>

    fun findBy(categoryId: String): Flow<Category?>

    suspend fun create(categoryId: String, categoryUpsert: CategoryUpsert)

    suspend fun update(categoryId: String, categoryUpsert: CategoryUpsert)

    suspend fun deleteBy(categoryId: String)
}