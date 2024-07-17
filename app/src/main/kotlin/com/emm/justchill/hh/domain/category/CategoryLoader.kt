package com.emm.justchill.hh.domain.category

import com.emm.justchill.Categories
import kotlinx.coroutines.flow.Flow

class CategoryLoader(private val repository: CategoryRepository) {

    fun load(): Flow<List<Categories>> {
        return repository.all()
    }
}