package com.emm.domain.category

class CategoryUpdater(private val repository: CategoryRepository) {

    suspend fun update(categoryId: String, categoryUpsert: CategoryUpsert) {
        repository.update(categoryId, categoryUpsert)
    }
}