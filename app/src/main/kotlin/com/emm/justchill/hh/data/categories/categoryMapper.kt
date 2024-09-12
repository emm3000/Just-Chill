package com.emm.justchill.hh.data.categories

import com.emm.justchill.Categories
import com.emm.justchill.hh.domain.categories.Category

fun Categories.toDomain() = Category(
    categoryId = categoryId,
    name = name,
    type = type,
    description = description.orEmpty(),
    syncStatus = syncStatus
)

fun List<Categories>.toDomain() = map(Categories::toDomain)

fun Category.toModel(userId: String) = CategoryModel(
    categoryId = categoryId,
    name = name,
    description = description,
    type = type,
    userId = userId
)