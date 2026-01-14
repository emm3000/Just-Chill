package com.emm.data.category

import com.emm.data.Categories
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryUpsert
import com.emm.domain.shared.SyncState

fun Categories.toDomain() = Category(
    categoryId = categoryId,
    name = name,
    icon = icon,
    color = color,
)

fun List<Categories>.toDomain() = map(Categories::toDomain)

fun Categories.toCategoryModel() = CategoryModel(
    categoryId = categoryId,
    name = name,
    updatedAt = updatedAt,
    createdAt = createdAt,
)

fun Categories.toCategoryUpsert() = CategoryUpsert(
    categoryId = categoryId,
    name = name,
    icon = icon,
    color = color,
    syncState = SyncState.Synced,
    updatedAt = updatedAt,
    createdAt = createdAt,
)

fun CategoryModel.toCategoryUpsert() = CategoryUpsert(
    categoryId = categoryId,
    name = name,
    icon = "icon",
    color = "color",
    syncState = SyncState.Synced,
    updatedAt = updatedAt,
    createdAt = createdAt
)
