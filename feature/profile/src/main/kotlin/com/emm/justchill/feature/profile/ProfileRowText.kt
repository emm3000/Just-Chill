package com.emm.justchill.feature.profile

import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.ui.format.format
import com.emm.justchill.core.ui.format.formatNeutral

fun categoriesMetaText(total: Int, incomeCount: Int): String = "$total en total · $incomeCount de ingreso"

fun recurringMetaText(activeCount: Int, monthlyOutflow: Money): String = when (activeCount) {
    0 -> "Ninguno todavía"
    else -> "$activeCount al mes · salen ${formatNeutral(monthlyOutflow.format())}"
}

fun LastExportUi.toMetaText(): String = when (this) {
    LastExportUi.Never -> "Nunca"
    is LastExportUi.DaysAgo -> "Último: ${daysAgoLabel(days)}"
}
