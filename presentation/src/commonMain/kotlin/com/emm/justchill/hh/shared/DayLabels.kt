package com.emm.justchill.hh.shared

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus

// The labels the UI puts on a day and on a time of day.
//
// There is nothing to convert here any more. A transaction's `occurredAt` is already a calendar
// day and a wall-clock time, so rendering it needs no timezone — it shows the day and the hour the
// user recorded, wherever the device happens to be now.

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

/** "9:05 a. m." — the wall-clock time the transaction was recorded at. */
fun timeLabel(time: LocalTime): String = SpanishDateFormat.readableTime(time.hour, time.minute)
