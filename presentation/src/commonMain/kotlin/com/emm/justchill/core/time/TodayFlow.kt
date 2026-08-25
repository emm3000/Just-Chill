package com.emm.justchill.core.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
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
 * passes underneath it. One binding, shared by every screen that has this problem — `SeeTransactions`
 * is the first, not the only one.
 */
fun interface TodayFlow {
    operator fun invoke(): Flow<LocalDate>
}

/**
 * Emits today's date, then sleeps until the next local midnight and emits again, forever. A day
 * boundary is the event this flow reacts to, not a poll — nothing wakes it up in between.
 * `distinctUntilChanged` guards a clock that ever moves backward from replaying the same date twice.
 */
class ClockTodayFlow(private val clock: Clock, private val zone: TimeZone) : TodayFlow {

    override fun invoke(): Flow<LocalDate> = flow {
        while (true) {
            val now = clock.now()
            val date = now.toLocalDateTime(zone).date
            emit(date)
            delay(date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone) - now)
        }
    }.distinctUntilChanged()
}
