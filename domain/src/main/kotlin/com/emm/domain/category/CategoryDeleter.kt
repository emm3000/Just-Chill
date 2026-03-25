package com.emm.domain.category

class DeleteCategoryUseCase(private val repository: CategoryRepository) {

    suspend operator fun invoke(categoryId: String) {
        repository.delete(categoryId)
    }
}
