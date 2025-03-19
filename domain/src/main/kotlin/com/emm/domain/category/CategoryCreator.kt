package com.emm.domain.category

class CategoryCreator(private val repository: CategoryRepository) {

    suspend fun create(categoryUpsert: CategoryUpsert) {
        repository.create(categoryUpsert)
    }
}