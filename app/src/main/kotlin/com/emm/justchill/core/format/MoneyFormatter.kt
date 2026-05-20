package com.emm.justchill.core.format

import com.emm.domain.shared.Money
import java.text.NumberFormat
import java.util.Locale

private const val CENTS_PER_UNIT = 100.0

/**
 * Formats a [Money] value as a locale-formatted decimal string (e.g. "1.234,56" for es-PE).
 * Does NOT include a currency symbol — use [com.emm.justchill.hh.shared.formatIncome],
 * [formatExpense], or [formatNeutral] to prepend the symbol.
 */
fun Money.format(locale: Locale = Locale("es", "PE")): String {
    val formatter = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return formatter.format(cents / CENTS_PER_UNIT)
}
