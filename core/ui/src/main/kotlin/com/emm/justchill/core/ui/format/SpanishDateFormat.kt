package com.emm.justchill.core.ui.format

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.number

// Locale tables are hardcoded to match, byte-for-byte, the JVM es/es-PE formatter output
// (SpanishFormatGoldenTest pins it). This is the only Spanish month table in the app —
// MonthLabelsTest fails the build if a second one reappears.
object SpanishDateFormat {

    // Index 1..12; index 0 is an unused placeholder.
    private val FULL_MONTHS: Array<String> = arrayOf(
        "", "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )

    // "sept" is 4 characters, matching the JVM es output.
    private val SHORT_MONTHS: Array<String> = arrayOf(
        "", "ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sept", "oct", "nov", "dic",
    )

    // ISO index 1 (Monday)..7 (Sunday); index 0 is an unused placeholder.
    private val FULL_WEEKDAYS: Array<String> = arrayOf(
        "",
        "lunes",
        "martes",
        "miércoles",
        "jueves",
        "viernes",
        "sábado",
        "domingo",
    )

    fun fullMonth(month: Month): String = FULL_MONTHS[month.number]

    fun shortMonth(month: Month): String = SHORT_MONTHS[month.number]

    fun fullWeekday(isoDayNumber: Int): String = FULL_WEEKDAYS[isoDayNumber]

    fun longDate(date: LocalDate): String = "${date.dayOfMonth} de ${fullMonth(date.month)} de ${date.year}"

    fun dayShortMonth(date: LocalDate): String = "${date.dayOfMonth} ${shortMonth(date.month)}"

    fun time(time: LocalTime): String {
        val hour: String = time.hour.toString().padStart(2, '0')
        val minute: String = time.minute.toString().padStart(2, '0')
        return "$hour:$minute"
    }

    fun monthDayPadded(date: LocalDate): String {
        val day = date.dayOfMonth.toString().padStart(2, '0')
        return "${fullMonth(date.month)} $day"
    }

    fun monthYear(year: Int, month: Month): String = "${fullMonth(month)} $year"

    fun dayFullMonth(date: LocalDate): String = "${date.dayOfMonth} ${fullMonth(date.month)}"
}

// Titlecases only the first character, with no locale — safe because the app is Spanish-only and
// the Latin alphabet needs none.
fun String.titlecaseFirstChar(): String = if (isEmpty()) this else this[0].uppercaseChar() + substring(1)
