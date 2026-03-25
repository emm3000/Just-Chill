package com.emm.domain.category

import kotlinx.coroutines.flow.firstOrNull

class FindCategoryUseCase(private val repository: CategoryRepository) {

    suspend operator fun invoke(categoryId: String): Category? {
        return repository.find(categoryId).firstOrNull()
    }
}
