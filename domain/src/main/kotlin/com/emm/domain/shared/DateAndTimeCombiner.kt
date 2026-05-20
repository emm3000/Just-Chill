package com.emm.domain.shared

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class DateAndTimeCombiner {

    fun combineWithUtc(dateInMillis: Long): Long {
        val selectedDateTime: kotlinx.datetime.LocalDateTime = Instant
            .fromEpochMilliseconds(dateInMillis)
            .toLocalDateTime(TimeZone.UTC)

        val systemZone: TimeZone = TimeZone.currentSystemDefault()
        val currentTime = Clock.System.now().toLocalDateTime(systemZone).time

        val combinedDateTime = LocalDateTime(selectedDateTime.date, currentTime)

        return combinedDateTime.toInstant(systemZone).toEpochMilliseconds()
    }

}
