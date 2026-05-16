package com.emm.data.category

import com.emm.data.Categories
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
fun CategoryEntity.asExternalModel() = Category(
    categoryId = CategoryId(categoryId),
    name = name,
    icon = icon,
    color = color,
    categoryType = CategoryType.valueOf(categoryType),
)

fun List<CategoryEntity>.asExternalModel() = map(CategoryEntity::asExternalModel)

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
