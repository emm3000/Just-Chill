package com.emm.justchill.feature.category

import com.emm.justchill.core.domain.category.Category

data class CategoryRowUi(val category: Category, val movementCount: Int) {

    val id: String get() = category.categoryId.value

    val name: String get() = category.name

    val iconId: String get() = category.icon

    val colorId: String get() = category.color

    val movementCountLabel: String get() = movementCountLabel(movementCount)
}

internal fun movementCountLabel(count: Int): String = "$count mov."
