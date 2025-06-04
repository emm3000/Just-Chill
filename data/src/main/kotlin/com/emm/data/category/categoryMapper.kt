package com.emm.data.category

import com.emm.data.Categories
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryUpsert

fun Categories.toDomain() = Category(
    categoryId = categoryId,
    name = name,
)

fun List<Categories>.toDomain() = map(Categories::toDomain)

fun Categories.toCategoryModel() = CategoryModel(
    categoryId = categoryId,
    name = name,
    updatedAt = updatedAt,
)

fun Categories.toCategoryUpsert() = CategoryUpsert(
    categoryId = categoryId,
    name = name,
    isSynced = true,
    updatedAt = updatedAt,
)

fun CategoryModel.toCategoryUpsert() = CategoryUpsert(
    categoryId = categoryId,
    name = name,
    isSynced = true,
    updatedAt = updatedAt,
)
