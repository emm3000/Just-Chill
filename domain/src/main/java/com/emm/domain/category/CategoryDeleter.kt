package com.emm.domain.category

class CategoryDeleter(
    private val repository: CategoryRepository,
) {

    suspend fun delete(categoryId: String) {
        repository.deleteBy(categoryId)
    }
}