package com.emm.justchill.hh.transaction

import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.CategoryId

data class SelectableCategory(
    val categoryId: CategoryId,
    val name: String,
    val iconId: String,
    val categoryType: CategoryType,
    val colorId: String,
)

fun Category.toSelectable(): SelectableCategory = SelectableCategory(
    categoryId = categoryId,
    name = name,
    iconId = icon,
    categoryType = categoryType,
    colorId = color,
)
