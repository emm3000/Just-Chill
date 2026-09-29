package com.emm.justchill.core.domain.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

interface TodayFlow {

    fun today(): LocalDate

    operator fun invoke(): Flow<LocalDate>
}

// resumeEvents is not redundant: delay on Android is scheduled against SystemClock.uptimeMillis(),
// which does not advance in deep sleep, so a phone dozing across midnight fires the wake late by
// the whole doze duration. distinctUntilChanged swallows the late wake once it finally arrives.
class ClockTodayFlow(private val clock: Clock, private val zone: TimeZone, private val resumeEvents: Flow<Unit>) :
    TodayFlow {

    override fun today(): LocalDate = clock.now().toLocalDateTime(zone).date

    override fun invoke(): Flow<LocalDate> = merge(midnights(), resumeEvents.map { today() })
        .distinctUntilChanged()

    private fun midnights(): Flow<LocalDate> = flow {
        while (true) {
            val now = clock.now()
            val date = now.toLocalDateTime(zone).date
            emit(date)
            delay(date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone) - now)
        }
    }
}
