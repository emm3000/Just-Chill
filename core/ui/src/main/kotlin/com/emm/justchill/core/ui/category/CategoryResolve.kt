package com.emm.justchill.core.ui.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.ui.graphics.vector.ImageVector

val CategoryUi.resolvedIcon: ImageVector
    get() = iconId?.let(AppIconCatalog::findById)?.icon ?: Icons.Rounded.QuestionMark

val CategoryUi.resolvedColor: CategoryColor
    get() = findById(colorId ?: "gray")

val SelectableCategory.resolvedIcon: ImageVector
    get() = AppIconCatalog.findById(iconId).icon

val SelectableCategory.resolvedColor: CategoryColor
    get() = findById(colorId)
