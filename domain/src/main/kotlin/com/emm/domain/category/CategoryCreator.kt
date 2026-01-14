package com.emm.domain.category

import com.emm.domain.shared.SyncState
import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.shared.currentTimeInMillis

class CategoryCreator(
    private val repository: CategoryRepository,
    private val idProvider: UniqueIdProvider,
) {

    suspend fun create(
        name: String,
        icon: String,
        color: String,
    ) {
        val categoryUpsert = CategoryUpsert(
            categoryId = idProvider.id,
            name = name,
            icon = icon,
            color = color,
            updatedAt = currentTimeInMillis(),
            createdAt = currentTimeInMillis(),
            syncState = SyncState.Pending,
        )
        repository.create(categoryUpsert)
    }
}