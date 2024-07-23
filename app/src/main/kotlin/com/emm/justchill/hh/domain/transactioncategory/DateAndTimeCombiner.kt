package com.emm.justchill.hh.domain.transactioncategory

import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class DateAndTimeCombiner {

    fun combine(dateInMillis: Long): Long {

        val selectedDateTime: LocalDateTime = LocalDateTime
            .ofInstant(
                Instant.ofEpochMilli(dateInMillis),
                ZoneId.of("UTC")
            )

        val currentTime: LocalTime = LocalTime.now()

        val combinedDateTime: LocalDateTime = selectedDateTime.with(currentTime)

        val combinedDateTimeInMillis: Long = combinedDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        return combinedDateTimeInMillis
    }
}