package com.emm.justchill.hh.transaction

import com.emm.domain.category.CategoryType
import com.emm.domain.shared.CategoryId
import com.emm.justchill.hh.category.CategoryColor
import com.emm.justchill.hh.category.IconCatalog

data class SelectableCategory(
    val categoryId: CategoryId,
    val name: String,
    val icon: IconCatalog,
    val categoryType: CategoryType,
    val color: CategoryColor,
)