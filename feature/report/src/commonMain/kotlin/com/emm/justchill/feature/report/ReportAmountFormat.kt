package com.emm.justchill.feature.report

import com.emm.justchill.core.presentation.format.NumberFormatEs
import com.emm.justchill.core.presentation.format.formatNeutral

internal fun formatSoles(cents: Long): String {
    val soles: Double = cents.toDouble() / 100.0
    return formatNeutral(NumberFormatEs.integerRounded(soles))
}

internal fun formatSolesWithDecimals(cents: Long): String = formatNeutral(NumberFormatEs.cents(cents))
