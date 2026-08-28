package com.emm.justchill.hh.shared

import com.emm.domain.shared.Money
import com.emm.justchill.hh.profile.LastExportUi

fun categoriesMetaText(total: Int, incomeCount: Int): String = "$total en total · $incomeCount de ingreso"

fun recurringMetaText(activeCount: Int, monthlyOutflow: Money): String = when (activeCount) {
    0 -> "Ninguno todavía"
    else -> "$activeCount al mes · salen ${formatNeutral(fromCentsToSolesWith(monthlyOutflow))}"
}

fun LastExportUi.toMetaText(): String = when (this) {
    LastExportUi.Never -> "Nunca"
    is LastExportUi.DaysAgo -> "Último: ${daysAgoLabel(days)}"
}
