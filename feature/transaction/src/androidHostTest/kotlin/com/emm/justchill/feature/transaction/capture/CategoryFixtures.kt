package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.presentation.category.SelectableCategory

internal val market: SelectableCategory = selectableCategory("market", "Supermercado")
internal val taxi: SelectableCategory = selectableCategory("taxi", "Taxi")
internal val coffee: SelectableCategory = selectableCategory("coffee", "Café")

internal fun selectableCategory(id: String, name: String): SelectableCategory = SelectableCategory(
    categoryId = CategoryId(id),
    name = name,
    iconId = "icon",
    categoryType = CategoryType.Spend,
    colorId = "color",
)
