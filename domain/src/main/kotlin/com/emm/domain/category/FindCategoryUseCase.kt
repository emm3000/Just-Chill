package com.emm.domain.category

import com.emm.domain.shared.CategoryId
import kotlinx.coroutines.flow.firstOrNull

class FindCategoryUseCase(private val repository: CategoryRepository) {

    suspend operator fun invoke(categoryId: CategoryId): Category? {
        return repository.find(categoryId).firstOrNull()
    }
}
