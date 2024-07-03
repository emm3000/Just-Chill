package com.emm.justchill.hh.presentation.transaction

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

object DateUtils {

    fun currentDateAtReadableFormat(): String {

        val currentLocalDate: LocalDate = LocalDate.now()

        val readableFormatter: DateTimeFormatter = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.LONG)
            .withLocale(Locale("es"))

        return currentLocalDate.format(readableFormatter)
    }

    fun millisToReadableFormat(millis: Long): String {

        val localDate: LocalDate = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val readableFormatter: DateTimeFormatter = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.LONG)
            .withLocale(Locale("es"))

        return localDate.format(readableFormatter) ?: String()
    }

    fun currentDateInMillis(): Long {
        val currentDate: LocalDate = LocalDate.now()

        return currentDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}