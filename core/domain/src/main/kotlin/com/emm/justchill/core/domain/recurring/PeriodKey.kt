package com.emm.justchill.core.domain.recurring

import com.emm.justchill.core.domain.shared.YearMonth
import kotlinx.datetime.Month
import kotlinx.datetime.number

private const val PERIOD_KEY_PARTS: Int = 2
private const val MIN_PERIOD_KEY_YEAR: Int = 1000
private const val MAX_PERIOD_KEY_YEAR: Int = 9999

fun periodKey(yearMonth: YearMonth): String {
    val mm: String = yearMonth.month.number.toString().padStart(2, '0')
    return "${yearMonth.year}-$mm"
}

fun parsePeriodKey(key: String): YearMonth? {
    val parts: List<String> = key.split('-')
    if (parts.size != PERIOD_KEY_PARTS) return null
    val year: Int? = parts[0].toIntOrNull()?.takeIf { it in MIN_PERIOD_KEY_YEAR..MAX_PERIOD_KEY_YEAR }
    val monthNumber: Int? = parts[1].toIntOrNull()?.takeIf { it in 1..Month.entries.size }
    return if (year != null && monthNumber != null) YearMonth(year, Month(monthNumber)) else null
}
