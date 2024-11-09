package com.emm.justchill.hh.category.domain

class CategoryDeleter(
    private val repository: CategoryRepository,
) {

    suspend fun delete(categoryId: String) {
        repository.deleteBy(categoryId)
    }
}