package com.emm.domain.category

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.UniqueIdProvider

class CreateCategoryUseCase(
    private val repository: CategoryRepository,
    private val idProvider: UniqueIdProvider,
) {

    suspend operator fun invoke(
        name: String,
        icon: String,
        color: String,
        categoryType: CategoryType,
    ) {
        val categoryUpsert = CategoryUpsert(
            categoryId = CategoryId(idProvider.id),
            name = name,
            icon = icon,
            color = color,
            categoryType = categoryType,
        )
        repository.create(categoryUpsert)
    }
}
