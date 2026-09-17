package com.emm.justchill.core.domain.category

import com.emm.justchill.core.domain.shared.CategoryId

data class Category(
    val categoryId: CategoryId,
    val name: String,
    val icon: String,
    val color: String,
    val categoryType: CategoryType,
)
