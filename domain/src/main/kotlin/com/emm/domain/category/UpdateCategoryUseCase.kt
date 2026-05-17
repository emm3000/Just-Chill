package com.emm.domain.category

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.error.DomainException

class UpdateCategoryUseCase(private val repository: CategoryRepository) {

    suspend operator fun invoke(categoryId: CategoryId, categoryUpsert: CategoryUpsert) {
        if (categoryUpsert.name.isBlank()) {
            throw DomainException.ValidationError("El nombre no puede estar vacío")
        }
        repository.update(categoryId, categoryUpsert)
    }
}
