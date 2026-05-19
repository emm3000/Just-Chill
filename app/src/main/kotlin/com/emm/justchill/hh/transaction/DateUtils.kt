package com.emm.justchill.hh.transaction

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val SPANISH = Locale.forLanguageTag("es")
private val SHORT_DATE = DateTimeFormatter.ofPattern("EEE d MMM", SPANISH)

object DateUtils {

    fun currentDateAtReadableFormat(formatStyle: FormatStyle = FormatStyle.LONG): String {

        val currentLocalDate: LocalDate = LocalDate.now()

        val readableFormatter: DateTimeFormatter = DateTimeFormatter
            .ofLocalizedDate(formatStyle)
            .withLocale(Locale.forLanguageTag("es"))

        return currentLocalDate.format(readableFormatter)
    }

    fun millisToReadableFormat(millis: Long): String {

        val localDate: LocalDate = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val readableFormatter: DateTimeFormatter = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.LONG)
            .withLocale(Locale.forLanguageTag("es"))

        return localDate.format(readableFormatter)
    }

    // This is ok for TransactionViewModel
    fun millisToReadableFormatUTC(millis: Long): String {

        val localDate: LocalDate = Instant.ofEpochMilli(millis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()

        val readableFormatter: DateTimeFormatter = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.LONG)
            .withLocale(Locale.forLanguageTag("es"))

        return localDate.format(readableFormatter)
    }

    fun currentDateInMillis(): Long {
        val currentDate: LocalDate = LocalDate.now()

        return currentDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    fun friendlyDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val date: LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        val today: LocalDate = LocalDate.now(zone)
        return when (date) {
            today -> "Hoy"
            today.minusDays(1) -> "Ayer"
            today.plusDays(1) -> "Mañana"
            else -> date.format(SHORT_DATE).replaceFirstChar { it.titlecase(SPANISH) }
        }
    }

    fun friendlyDateUTC(millis: Long): String = friendlyDate(millis, ZoneOffset.UTC)

    fun readableTime(millis: Long): String {
        val toLocalTime = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        return toLocalTime.format(formatter)
    }
}