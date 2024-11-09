package com.emm.justchill.hh.category.data

import com.emm.justchill.Categories
import com.emm.justchill.hh.category.domain.Category

fun Categories.toDomain() = Category(
    categoryId = categoryId,
    name = name,
    type = type,
    description = description.orEmpty(),
)

fun List<Categories>.toDomain() = map(Categories::toDomain)

fun Category.toModel(userId: String) = CategoryModel(
    categoryId = categoryId,
    name = name,
    description = description,
    type = type,
    userId = userId
)