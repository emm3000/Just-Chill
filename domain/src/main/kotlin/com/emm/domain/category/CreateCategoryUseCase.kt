package com.emm.domain.category

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.shared.error.DomainException

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
        if (name.isBlank()) {
            throw DomainException.ValidationError("El nombre no puede estar vacío")
        }
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
