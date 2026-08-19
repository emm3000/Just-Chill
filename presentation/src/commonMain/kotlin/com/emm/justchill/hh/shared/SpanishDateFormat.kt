package com.emm.justchill.hh.shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.number

/**
 * Locale tables are hardcoded to match, byte-for-byte, the JVM `es`/`es-PE` formatter output
 * (`SpanishFormatGoldenTest` pins it). This is the only Spanish month table in the app —
 * `MonthLabelsTest` fails the build if a second one reappears.
 */
object SpanishDateFormat {

    /** Full Spanish month names — "MMMM". Index 1..12. */
    private val FULL_MONTHS: Array<String> = arrayOf(
        "", "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )

    /** Short Spanish month names — "MMM". Note "sept" (4 chars), matching the JVM `es` output. */
    private val SHORT_MONTHS: Array<String> = arrayOf(
        "", "ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sept", "oct", "nov", "dic",
    )

    /** Full Spanish weekday names — DayOfWeek FULL. ISO index 1 (Monday)..7 (Sunday). */
    private val FULL_WEEKDAYS: Array<String> = arrayOf(
        "", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo",
    )

    fun fullMonth(month: Month): String = FULL_MONTHS[month.number]

    fun shortMonth(month: Month): String = SHORT_MONTHS[month.number]

    fun fullWeekday(isoDayNumber: Int): String = FULL_WEEKDAYS[isoDayNumber]

    fun longDate(date: LocalDate): String =
        "${date.dayOfMonth} de ${fullMonth(date.month)} de ${date.year}"

    fun dayShortMonth(date: LocalDate): String = "${date.dayOfMonth} ${shortMonth(date.month)}"

    fun dayShortMonthTime(dateTime: LocalDateTime): String {
        val hour = dateTime.hour.toString().padStart(2, '0')
        val minute = dateTime.minute.toString().padStart(2, '0')
        return "${dateTime.dayOfMonth} ${shortMonth(dateTime.month)}, $hour:$minute"
    }

    fun monthDayPadded(date: LocalDate): String {
        val day = date.dayOfMonth.toString().padStart(2, '0')
        return "${fullMonth(date.month)} $day"
    }

    fun monthYear(year: Int, month: Month): String = "${fullMonth(month)} $year"

    fun dayFullMonth(date: LocalDate): String = "${date.dayOfMonth} ${fullMonth(date.month)}"

    fun readableTime(hour24: Int, minute: Int): String {
        val isPm = hour24 >= 12
        val hour12 = when {
            hour24 % 12 == 0 -> 12
            else -> hour24 % 12
        }
        val mm = minute.toString().padStart(2, '0')
        val marker = if (isPm) "p. m." else "a. m."
        return "$hour12:$mm $marker"
    }
}

/**
 * Titlecases only the first character, with no locale — safe because the app is Spanish-only and
 * the Latin alphabet needs none.
 */
fun String.titlecaseFirstChar(): String =
    if (isEmpty()) this else this[0].uppercaseChar() + substring(1)
