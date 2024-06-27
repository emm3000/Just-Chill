package com.emm.justchill.experiences.hh.domain.category

import com.emm.justchill.Categories
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    suspend fun add(name: String, type: String)

    fun all(): Flow<List<Categories>>
}