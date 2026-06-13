package com.emm.justchill.hh.transaction

import com.emm.justchill.hh.shared.SpanishDateFormat
import com.emm.justchill.hh.shared.titlecaseFirstChar
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

object DateUtils {

    fun currentDateAtReadableFormat(): String {
        val today: LocalDate = today(TimeZone.currentSystemDefault())
        return SpanishDateFormat.longDate(today)
    }

    fun millisToReadableFormatUTC(millis: Long): String {
        val date: LocalDate = Instant.fromEpochMilliseconds(millis)
            .toLocalDateTime(TimeZone.UTC)
            .date
        return SpanishDateFormat.longDate(date)
    }

    fun currentDateInMillis(): Long {
        val zone = TimeZone.currentSystemDefault()
        return today(zone).atStartOfDayIn(zone).toEpochMilliseconds()
    }

    fun friendlyDate(millis: Long, zone: TimeZone = TimeZone.currentSystemDefault()): String {
        val date: LocalDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone).date
        val today: LocalDate = today(zone)
        return when (date) {
            today -> "Hoy"
            today.minus(1, DateTimeUnit.DAY) -> "Ayer"
            today.plus(1, DateTimeUnit.DAY) -> "Mañana"
            else -> SpanishDateFormat.dayShortMonth(date).titlecaseFirstChar()
        }
    }

    fun friendlyDateUTC(millis: Long): String = friendlyDate(millis, TimeZone.UTC)

    fun readableTime(millis: Long): String {
        val time = Instant.fromEpochMilliseconds(millis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .time
        return SpanishDateFormat.readableTime(time.hour, time.minute)
    }

    private fun today(zone: TimeZone): LocalDate =
        Clock.System.now().toLocalDateTime(zone).date
}
