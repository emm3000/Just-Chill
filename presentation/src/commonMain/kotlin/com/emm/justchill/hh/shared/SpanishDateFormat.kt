package com.emm.justchill.hh.shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.number

/**
 * Hand-rolled Spanish (es / es-PE) date and number formatting for Compose Multiplatform
 * commonMain. Replaces the JVM `java.time.format.DateTimeFormatter` + `java.text.*` localized
 * formatters that cannot run on iOS. The app is Spanish-only, so the locale tables are hardcoded
 * to match — byte-for-byte — the strings the JVM `es`/`es-PE` formatters produced (verified on the
 * build JDK). See `SpanishDateFormatTest` in `:app` for the golden assertions.
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

    /** DayOfWeek FULL Spanish name. [isoDayNumber] is 1 (Monday)..7 (Sunday). */
    fun fullWeekday(isoDayNumber: Int): String = FULL_WEEKDAYS[isoDayNumber]

    /** "d de MMMM de yyyy" — e.g. "13 de junio de 2026". JVM FormatStyle.LONG for es. */
    fun longDate(date: LocalDate): String =
        "${date.dayOfMonth} de ${fullMonth(date.month)} de ${date.year}"

    /** "d MMM" — e.g. "13 jun". */
    fun dayShortMonth(date: LocalDate): String = "${date.dayOfMonth} ${shortMonth(date.month)}"

    /**
     * "d MMM, HH:mm" — e.g. "13 sept, 09:05". Non-padded day, short month, 24-hour
     * zero-padded hour and minute. Matches the JVM `SimpleDateFormat("d MMM, HH:mm", es)` output.
     */
    fun dayShortMonthTime(dateTime: LocalDateTime): String {
        val hour = dateTime.hour.toString().padStart(2, '0')
        val minute = dateTime.minute.toString().padStart(2, '0')
        return "${dateTime.dayOfMonth} ${shortMonth(dateTime.month)}, $hour:$minute"
    }

    /** "MMMM dd" — e.g. "junio 13" (zero-padded day). */
    fun monthDayPadded(date: LocalDate): String {
        val day = date.dayOfMonth.toString().padStart(2, '0')
        return "${fullMonth(date.month)} $day"
    }

    /** "MMMM yyyy" — e.g. "septiembre 2026". */
    fun monthYear(year: Int, month: Month): String = "${fullMonth(month)} $year"

    /** "d MMMM" — e.g. "13 junio". */
    fun dayFullMonth(date: LocalDate): String = "${date.dayOfMonth} ${fullMonth(date.month)}"

    /**
     * 12-hour Spanish time "h:mm a" — e.g. "3:45 p. m.", "9:05 a. m.", "12:00 a. m." (midnight),
     * "12:00 p. m." (noon). [hour24] is 0..23.
     */
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
 * Titlecase only the first character, Spanish-style (locale-independent for the Latin alphabet the
 * app uses). Replaces `replaceFirstChar { it.titlecase(SPANISH) }`.
 */
fun String.titlecaseFirstChar(): String =
    if (isEmpty()) this else this[0].uppercaseChar() + substring(1)
