package com.emm.domain.category

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.error.DomainException

class UpdateCategoryUseCase(private val repository: CategoryRepository) {

    suspend operator fun invoke(categoryId: CategoryId, categoryUpsert: CategoryUpsert) {
        if (categoryUpsert.name.isBlank()) {
            throw DomainException.ValidationError("Name cannot be empty")
        }
        repository.update(categoryId, categoryUpsert)
    }
}
