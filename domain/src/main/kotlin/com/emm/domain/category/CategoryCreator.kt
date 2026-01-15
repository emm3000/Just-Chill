package com.emm.domain.category

import com.emm.domain.shared.UniqueIdProvider

class CategoryCreator(
    private val repository: CategoryRepository,
    private val idProvider: UniqueIdProvider,
) {

    suspend fun create(
        name: String,
        icon: String,
        color: String,
        categoryType: CategoryType,
    ) {
        val categoryUpsert = CategoryUpsert(
            categoryId = idProvider.id,
            name = name,
            icon = icon,
            color = color,
            categoryType = categoryType,
        )
        repository.create(categoryUpsert)
    }
}