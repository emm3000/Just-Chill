package com.emm.justchill.core.ui.atoms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.ui.graphics.vector.ImageVector

enum class MonthChevronDirection(val icon: ImageVector, val contentDescription: String) {
    Previous(Icons.Outlined.ChevronLeft, "Mes anterior"),
    Next(Icons.Outlined.ChevronRight, "Mes siguiente"),
}
