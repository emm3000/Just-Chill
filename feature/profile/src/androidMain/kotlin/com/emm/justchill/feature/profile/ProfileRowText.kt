package com.emm.justchill.feature.profile

fun categoriesMetaText(total: Int, incomeCount: Int): String = "$total en total · $incomeCount de ingreso"

fun LastExportUi.toMetaText(): String = when (this) {
    LastExportUi.Never -> "Nunca"
    is LastExportUi.DaysAgo -> "Último: ${daysAgoLabel(days)}"
}
