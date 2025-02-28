package com.emm.justchill.hh.category.data

interface CategoryRemoteRepository {

    suspend fun upsert(category: CategoryModel)

    suspend fun retrieve(): List<CategoryModel>

    suspend fun deleteBy(categoryId: String)

    suspend fun deleteAll()
}