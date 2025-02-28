package com.emm.domain.category

import com.emm.domain.shared.UniqueIdProvider

class CategoryCreator(
    private val repository: CategoryRepository,
    private val uniqueIdProvider: UniqueIdProvider,
) {

    suspend fun create(categoryUpsert: CategoryUpsert) {

        val uniqueId: String = uniqueIdProvider.uniqueId

        repository.create(uniqueId, categoryUpsert)
    }
}