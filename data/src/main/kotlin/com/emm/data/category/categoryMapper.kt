package com.emm.data.category

import com.emm.data.Categories
import com.emm.domain.category.Category

fun Categories.toDomain() = Category(
    categoryId = categoryId,
    name = name,
)

fun List<Categories>.toDomain() = map(Categories::toDomain)
