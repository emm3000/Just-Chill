package com.emm.data.category

import com.emm.data.Categories
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryType
import com.emm.domain.category.CategoryUpsert

// SQLDelight -> Entity (internal, stays within data source)
fun Categories.asEntity() = CategoryEntity(
    categoryId = categoryId,
    name = name,
    icon = icon,
    color = color,
    categoryType = categoryType,
    syncState = syncState,
    isDefault = isDefault,
    isDeleted = isDeleted,
    updatedAt = updatedAt,
    createdAt = createdAt,
)

fun List<Categories>.asEntity() = map(Categories::asEntity)

// Entity -> Domain
fun CategoryEntity.asExternalModel() = Category(
    categoryId = categoryId,
    name = name,
    icon = icon,
    color = color,
    categoryType = CategoryType.valueOf(categoryType),
)

fun List<CategoryEntity>.asExternalModel() = map(CategoryEntity::asExternalModel)

// Domain upsert -> Entity
fun CategoryUpsert.asEntity() = CategoryEntity(
    categoryId = categoryId,
    name = name,
    icon = icon,
    color = color,
    categoryType = categoryType.name,
    syncState = "",
    isDefault = false,
    isDeleted = false,
    updatedAt = 0L,
    createdAt = 0L,
)

// Network -> Entity
fun NetworkCategory.asEntity() = CategoryEntity(
    categoryId = categoryId,
    name = name,
    icon = "icon",
    color = "color",
    categoryType = CategoryType.Income.name,
    syncState = "",
    isDefault = false,
    isDeleted = false,
    updatedAt = updatedAt,
    createdAt = createdAt,
)

// Entity -> Network
fun CategoryEntity.asNetworkModel(userId: String) = NetworkCategory(
    categoryId = categoryId,
    name = name,
    updatedAt = updatedAt,
    createdAt = createdAt,
    userId = userId,
)
