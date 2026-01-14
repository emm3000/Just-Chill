package com.emm.justchill.hh.transaction

import com.emm.justchill.hh.category.CategoryColor
import com.emm.justchill.hh.category.IconCatalog

data class SelectableCategory(
    val categoryId: String,
    val name: String,
    val icon: IconCatalog,
    val color: CategoryColor,
)