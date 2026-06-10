package com.emm.data.category

import com.emm.data.Categories
import com.emm.data.shared.enumValueOrNull
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryType
import com.emm.domain.category.CategoryUpsert
import com.emm.domain.shared.CategoryId

// SQLDelight -> Entity (internal, stays within data source)
fun Categories.asEntity() = CategoryEntity(
    categoryId = categoryId,
    name = name,
    icon = icon,
    color = color,
    categoryType = categoryType,
    isDefault = isDefault,
    updatedAt = updatedAt,
    createdAt = createdAt,
)

fun List<Categories>.asEntity() = map(Categories::asEntity)

// Entity -> Domain
fun CategoryEntity.asExternalModelOrNull(): Category? {
    val parsedType = enumValueOrNull<CategoryType>(categoryType) ?: return null
    return Category(
        categoryId = CategoryId(categoryId),
        name = name,
        icon = icon,
        color = color,
        categoryType = parsedType,
    )
}

fun List<CategoryEntity>.asExternalModel() = mapNotNull(CategoryEntity::asExternalModelOrNull)

// Domain upsert -> Entity
fun CategoryUpsert.asEntity() = CategoryEntity(
    categoryId = categoryId.value,
    name = name,
    icon = icon,
    color = color,
    categoryType = categoryType.name,
    isDefault = false,
    updatedAt = 0L,
    createdAt = 0L,
)
