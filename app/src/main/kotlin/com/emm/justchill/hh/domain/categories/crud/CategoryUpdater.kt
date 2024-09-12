package com.emm.justchill.hh.domain.categories.crud

import com.emm.justchill.hh.domain.categories.CategoryRepository
import com.emm.justchill.hh.domain.categories.CategoryUpsert

class CategoryUpdater(
    private val repository: CategoryRepository,
) {

    suspend fun update(categoryId: String, categoryUpsert: CategoryUpsert) {
        repository.update(categoryId, categoryUpsert)
    }
}