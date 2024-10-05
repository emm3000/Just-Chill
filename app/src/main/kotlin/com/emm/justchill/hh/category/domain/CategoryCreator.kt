package com.emm.justchill.hh.category.domain

import com.emm.justchill.hh.shared.UniqueIdProvider

class CategoryCreator(
    private val repository: CategoryRepository,
    private val uniqueIdProvider: UniqueIdProvider,
) {

    suspend fun create(categoryUpsert: CategoryUpsert) {

        val uniqueId: String = uniqueIdProvider.uniqueId

        repository.create(uniqueId, categoryUpsert)
    }
}