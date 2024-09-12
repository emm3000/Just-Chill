package com.emm.justchill.hh.domain.categories.crud

import com.emm.justchill.hh.domain.categories.Category
import com.emm.justchill.hh.domain.categories.CategoryRepository
import kotlinx.coroutines.flow.firstOrNull

class CategoryFinder(private val repository: CategoryRepository) {

    suspend fun find(categoryId: String): Category? {
        return repository.findBy(categoryId).firstOrNull()
    }
}