package com.emm.justchill.hh.data.category

import com.emm.justchill.hh.domain.CategoryModel

interface CategoryNetworkDataSource {

    suspend fun upsert(category: CategoryModel)

    suspend fun upsert(categories: List<CategoryModel>)

    suspend fun retrieve(): List<CategoryModel>
}