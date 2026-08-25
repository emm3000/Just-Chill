package com.emm.justchill.core.time

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

/**
 * Today's date, kept current on its own. A screen that shows HOY/AYER labels or a pending-movement
 * list needs this instead of a one-shot read: the day it opened on goes stale the moment midnight
 * passes underneath it.
 *
 * [today] answers the same question synchronously, for an initial state or a `MutableStateFlow`
 * seed. It exists so a consumer of the stream never needs a `Clock` and a `TimeZone` of its own to
 * derive the date a second, independent way.
 */
interface TodayFlow {

    fun today(): LocalDate

    operator fun invoke(): Flow<LocalDate>
}

/**
 * Emits today's date, then sleeps until the next local midnight and emits again, forever. A day
 * boundary is the event this flow reacts to, not a poll — nothing wakes it up in between.
 *
 * [resumeEvents] is the second event, and it is not redundant: `delay` on Android is scheduled
 * against `SystemClock.uptimeMillis()`, which does not advance in deep sleep, so a phone that dozes
 * across midnight fires the midnight wake late by the whole doze duration. `distinctUntilChanged`
 * swallows a resume that lands on an unchanged date, and the late wake once it finally arrives.
 */
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
