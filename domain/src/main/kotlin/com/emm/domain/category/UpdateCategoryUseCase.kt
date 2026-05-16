package com.emm.domain.category

import com.emm.domain.shared.CategoryId

class UpdateCategoryUseCase(private val repository: CategoryRepository) {

    suspend operator fun invoke(categoryId: CategoryId, categoryUpsert: CategoryUpsert) {
        repository.update(categoryId, categoryUpsert)
    }
}
