package com.emm.data.category

class RemoteCategoryDataSource {

    suspend fun upsert(category: CategoryModel) {}

    suspend fun retrieve(): List<CategoryModel> {
        return listOf()
    }

    suspend fun deleteBy(categoryId: String) {}

    suspend fun deleteAll() {}
}