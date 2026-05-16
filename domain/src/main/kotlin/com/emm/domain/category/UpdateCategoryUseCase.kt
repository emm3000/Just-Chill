package com.emm.domain.category

class UpdateCategoryUseCase(private val repository: CategoryRepository) {

    suspend operator fun invoke(categoryId: String, categoryUpsert: CategoryUpsert) {
        repository.update(categoryId, categoryUpsert)
    }
}
