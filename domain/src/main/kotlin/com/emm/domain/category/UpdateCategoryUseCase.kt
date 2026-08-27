package com.emm.domain.category

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode

class UpdateCategoryUseCase(private val repository: CategoryRepository) {

    suspend operator fun invoke(categoryId: CategoryId, categoryUpsert: CategoryUpsert) {
        if (categoryUpsert.name.isBlank()) {
            throw DomainException.ValidationError("Name cannot be empty", ValidationCode.NameRequired)
        }
        repository.update(categoryId, categoryUpsert)
    }
}
