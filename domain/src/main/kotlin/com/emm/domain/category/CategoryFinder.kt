package com.emm.domain.category

import kotlinx.coroutines.flow.firstOrNull

class CategoryFinder(private val repository: CategoryRepository) {

    suspend fun find(categoryId: String): Category? {
        return repository.find(categoryId).firstOrNull()
    }
}