package com.emm.justchill.hh.transaction

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.ui.graphics.vector.ImageVector
import com.emm.justchill.hh.category.AppIconCatalog
import com.emm.justchill.hh.category.CategoryColor
import com.emm.justchill.hh.category.findById

// Render-time resolution of the semantic ids carried by presentation models. The ids are the
// plain strings the domain stores (Category.icon / Category.color); resolving them into
// ImageVector/CategoryColor here — instead of in the mappers — keeps :presentation compose-free.

val CategoryUi.resolvedIcon: ImageVector
    get() = iconId?.let(AppIconCatalog::findById)?.icon ?: Icons.Rounded.QuestionMark

val CategoryUi.resolvedColor: CategoryColor
    get() = findById(colorId ?: "gray")

val SelectableCategory.resolvedIcon: ImageVector
    get() = AppIconCatalog.findById(iconId).icon

val SelectableCategory.resolvedColor: CategoryColor
    get() = findById(colorId)
