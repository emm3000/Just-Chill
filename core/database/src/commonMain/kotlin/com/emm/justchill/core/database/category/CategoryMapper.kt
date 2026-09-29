package com.emm.justchill.core.database.category

import com.emm.justchill.core.database.Categories
import com.emm.justchill.core.database.shared.enumValueOrNull
import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.category.CategoryUpsert
import com.emm.justchill.core.domain.shared.CategoryId

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
