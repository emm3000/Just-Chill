package com.emm.justchill.hh.category.data

import com.emm.justchill.Categories
import com.emm.domain.category.Category

fun Categories.toDomain() = Category(
    categoryId = categoryId,
    name = name,
    type = type,
    description = description.orEmpty(),
)

fun List<Categories>.toDomain() = map(Categories::toDomain)
