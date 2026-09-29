package com.emm.justchill.feature.category

import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.CategoryId

internal fun category(id: String, name: String, type: CategoryType = CategoryType.Spend): Category =
    Category(categoryId = CategoryId(id), name = name, icon = "taxi", color = "green", categoryType = type)
