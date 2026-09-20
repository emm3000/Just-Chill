package com.emm.justchill.core.ui.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.emm.justchill.core.ui.theme.EmmColors

val selectableColorIds: List<String> = listOf("blue", "green", "red", "purple", "orange", "gray")

fun EmmColors.resolvedColor(colorId: String?): Color = when (colorId) {
    "blue" -> catSlate
    "green", "teal" -> catSage
    "red", "brown" -> catTerracotta
    "purple", "pink" -> catMauve
    "orange", "yellow" -> catOchre
    else -> catGraphite
}

val CategoryUi.resolvedIcon: ImageVector
    get() = iconId?.let(AppIconCatalog::findById)?.icon ?: Icons.Rounded.QuestionMark

val SelectableCategory.resolvedIcon: ImageVector
    get() = AppIconCatalog.findById(iconId).icon
