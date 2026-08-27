package com.emm.justchill.hh.report

import androidx.compose.ui.graphics.Color
import com.emm.justchill.core.theme.emmDarkColors

internal fun domainColorToUi(color: String?): Color = when (color) {
    "green" -> emmDarkColors.catSage
    "blue" -> emmDarkColors.catSlate
    "purple" -> emmDarkColors.catMauve
    "orange" -> emmDarkColors.catOchre
    "red" -> emmDarkColors.catTerracotta
    "brown" -> emmDarkColors.catTerracotta
    "yellow" -> emmDarkColors.catOchre
    "teal" -> emmDarkColors.catSage
    "pink" -> emmDarkColors.catMauve
    "gray" -> emmDarkColors.catGraphite
    else -> emmDarkColors.catGraphite
}
