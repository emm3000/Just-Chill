package com.emm.justchill.experiences.hh.presentation.transaction

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

        return currentLocalDate.format(readableFormatter) ?: String()
    }

    fun millisToReadableFormat(millis: Long): String {

        val localDate: LocalDate = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.of("UTC"))
            .toLocalDate()

        val readableFormatter: DateTimeFormatter = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.LONG)
            .withLocale(Locale("es"))

        return localDate.format(readableFormatter) ?: String()
    }

}