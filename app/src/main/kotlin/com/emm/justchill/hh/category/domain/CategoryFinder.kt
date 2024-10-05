package com.emm.justchill.hh.category.domain

import kotlinx.coroutines.flow.firstOrNull

class CategoryFinder(private val repository: CategoryRepository) {

    suspend fun find(categoryId: String): Category? {
        return repository.findBy(categoryId).firstOrNull()
    }
}