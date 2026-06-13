package com.emm.justchill.hh.transaction

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.emm.justchill.hh.category.CategoryColor

@Immutable
data class CategoryUi(val categoryIcon: ImageVector, val categoryColor: CategoryColor)
