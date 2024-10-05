package com.emm.justchill.hh.category.domain

class CategoryUpdater(
    private val repository: CategoryRepository,
) {

    suspend fun update(categoryId: String, categoryUpsert: CategoryUpsert) {
        repository.update(categoryId, categoryUpsert)
    }
}