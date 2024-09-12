package com.emm.justchill.hh.domain.categories

import com.emm.justchill.hh.domain.transaction.SyncStatus
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun retrieve(): Flow<List<Category>>

    fun findBy(categoryId: String): Flow<Category?>

    suspend fun create(categoryId: String, categoryUpsert: CategoryUpsert)

    suspend fun update(categoryId: String, categoryUpsert: CategoryUpsert)

    suspend fun updateStatus(categoryId: String, syncStatus: SyncStatus)

    suspend fun deleteBy(categoryId: String)
}