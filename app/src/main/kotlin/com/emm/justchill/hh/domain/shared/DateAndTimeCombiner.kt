package com.emm.justchill.hh.domain.shared

import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class DateAndTimeCombiner {

    fun combineWithUtc(dateInMillis: Long): Long {

        val selectedDateTime: LocalDateTime = LocalDateTime
            .ofInstant(
                Instant.ofEpochMilli(dateInMillis),
                ZoneId.of("UTC")
            )

        val currentTime: LocalTime = LocalTime.now(ZoneId.systemDefault())

        val combinedDateTime: LocalDateTime = selectedDateTime.with(currentTime)

        val combinedDateTimeInMillis: Long = combinedDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        return combinedDateTimeInMillis
    }

    fun combineDefaultZone(dateInMillis: Long): Long {

        val selectedDateTime: LocalDateTime = LocalDateTime
            .ofInstant(
                Instant.ofEpochMilli(dateInMillis),
                ZoneId.systemDefault(),
            )

        val currentTime: LocalTime = LocalTime.now(ZoneId.systemDefault())

        val combinedDateTime: LocalDateTime = selectedDateTime.with(currentTime)

        return combinedDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}