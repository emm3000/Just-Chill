package com.emm.justchill.hh.domain.categories.crud

import com.emm.justchill.hh.domain.categories.CategoryRepository
import com.emm.justchill.hh.domain.categories.CategoryUpsert
import com.emm.justchill.hh.domain.shared.UniqueIdProvider

class CategoryCreator(
    private val repository: CategoryRepository,
    private val uniqueIdProvider: UniqueIdProvider,
) {

    suspend fun create(categoryUpsert: CategoryUpsert) {

        val uniqueId: String = uniqueIdProvider.uniqueId

        repository.create(uniqueId, categoryUpsert)
    }
}