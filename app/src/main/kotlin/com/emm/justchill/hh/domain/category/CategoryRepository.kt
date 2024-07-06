package com.emm.justchill.hh.domain.category

import com.emm.justchill.Categories
import com.emm.justchill.hh.domain.CategoryModel
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    suspend fun add(name: String, type: String)

    fun all(): Flow<List<Categories>>

    suspend fun seed(data: List<CategoryModel>)
}