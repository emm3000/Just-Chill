package com.emm.domain.category

import com.emm.domain.shared.UniqueIdProvider

class CategoryCreator(private val repository: CategoryRepository) {

    suspend fun create(categoryUpsert: CategoryUpsert) {
        repository.create(uniqueId, categoryUpsert)
    }
}