package com.emm.justchill.core.format

import com.emm.domain.shared.Money
import com.emm.justchill.hh.shared.NumberFormatEs

private const val CENTS_PER_UNIT = 100.0

/**
 * Formats a [Money] value as an es-PE decimal string (comma thousands, dot decimal, two fraction
 * digits — e.g. "1,234.56"). Does NOT include a currency symbol — use
 * [com.emm.justchill.hh.shared.formatIncome], [formatExpense], or [formatNeutral] to prepend it.
 */
fun Money.format(): String = NumberFormatEs.decimal2(cents / CENTS_PER_UNIT)
