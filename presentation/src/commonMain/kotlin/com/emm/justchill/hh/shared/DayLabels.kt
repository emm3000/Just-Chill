package com.emm.justchill.hh.shared

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus

fun relativeDayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Hoy"
    today.minus(1, DateTimeUnit.DAY) -> "Ayer"
    today.plus(1, DateTimeUnit.DAY) -> "Mañana"
    else -> SpanishDateFormat.dayShortMonth(date).titlecaseFirstChar()
}

fun timeLabel(time: LocalTime): String = SpanishDateFormat.readableTime(time.hour, time.minute)
