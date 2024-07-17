package com.emm.justchill.hh.presentation.transaction

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

object DateUtils {

    fun currentDateAtReadableFormat(formatStyle: FormatStyle = FormatStyle.LONG): String {

        val currentLocalDate: LocalDate = LocalDate.now()

        val readableFormatter: DateTimeFormatter = DateTimeFormatter
            .ofLocalizedDate(formatStyle)
            .withLocale(Locale("es"))

        return currentLocalDate.format(readableFormatter)
    }

    fun millisToReadableFormat(millis: Long): String {

        val localDate: LocalDate = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.of("UTC"))
            .toLocalDate()

        val readableFormatter: DateTimeFormatter = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.LONG)
            .withLocale(Locale("es"))

        return localDate.format(readableFormatter)
    }

    fun currentDateInMillis(): Long {
        val currentDate: LocalDate = LocalDate.now()

        return currentDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    fun readableTime(millis: Long): String {
        val toLocalTime = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        return toLocalTime.format(formatter)
    }
}