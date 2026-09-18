package com.emm.justchill.hh.report

import com.emm.justchill.core.ui.format.NumberFormatEs

internal fun formatSoles(cents: Long): String {
    val soles = cents.toDouble() / 100.0
    return "S/ ${NumberFormatEs.integerRounded(soles)}"
}

internal fun formatSolesWithDecimals(cents: Long): String = "S/ ${NumberFormatEs.cents(cents)}"
