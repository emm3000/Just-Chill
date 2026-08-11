package com.emm.justchill.hh.shared

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// The two conversions between an instant and the calendar day it belongs to, plus the labels the
// UI puts on them.
//
// `transactions.date` is stored as epoch millis — an instant — so which day it falls on is a
// question only a timezone can answer, and every crossing of that boundary goes through here.

/** The calendar day the instant at [epochMillis] falls on, as read in [zone]. */
fun localDateOf(epochMillis: Long, zone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date

/** Midnight at the start of [date] in [zone], as epoch millis. Inverse of [localDateOf]. */
fun startOfDayMillis(date: LocalDate, zone: TimeZone = TimeZone.currentSystemDefault()): Long =
    date.atStartOfDayIn(zone).toEpochMilliseconds()

/**
 * "Hoy" / "Ayer" / "Mañana" for the three days nobody needs a calendar for, otherwise "13 ago".
 *
 * [today] is a parameter rather than an ambient `Clock.System.now()` read, for the same reason
 * `DayGroup` takes one: the caller reads the clock once per mapping pass, so every row in a single
 * emission agrees on which day is "Hoy". Reading the clock in here let a screen disagree with
 * itself, and let a list rendered at 23:59 keep claiming "Hoy" after midnight.
 */
fun relativeDayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Hoy"
    today.minus(1, DateTimeUnit.DAY) -> "Ayer"
    today.plus(1, DateTimeUnit.DAY) -> "Mañana"
    else -> SpanishDateFormat.dayShortMonth(date).titlecaseFirstChar()
}

/** "9:05 a. m." — the clock time of [epochMillis] as read in [zone]. */
fun timeLabel(epochMillis: Long, zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val time = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).time
    return SpanishDateFormat.readableTime(time.hour, time.minute)
}
