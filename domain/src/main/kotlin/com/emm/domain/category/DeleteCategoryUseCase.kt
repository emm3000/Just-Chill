package com.emm.domain.category

import com.emm.domain.shared.CategoryId

class DeleteCategoryUseCase(private val repository: CategoryRepository) {

    suspend operator fun invoke(categoryId: CategoryId) {
        repository.delete(categoryId)
    }
}
