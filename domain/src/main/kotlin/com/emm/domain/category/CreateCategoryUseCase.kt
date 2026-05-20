package com.emm.domain.category

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.shared.error.DomainException

class CreateCategoryUseCase(private val repository: CategoryRepository, private val idProvider: UniqueIdProvider) {

    suspend operator fun invoke(name: String, icon: String, color: String, categoryType: CategoryType): Category {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            throw DomainException.ValidationError("El nombre no puede estar vacío")
        }
        val categoryId = CategoryId(idProvider.id)
        repository.create(
            CategoryUpsert(
                categoryId = categoryId,
                name = trimmed,
                icon = icon,
                color = color,
                categoryType = categoryType,
            ),
        )
        return Category(
            categoryId = categoryId,
            name = trimmed,
            icon = icon,
            color = color,
            categoryType = categoryType,
        )
    }
}
