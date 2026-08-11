package com.emm.domain.shared

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Epoch millis at the start of the day [days] calendar days before today in [zone] — the inclusive
 * lower bound of a "last N days" window.
 *
 * Counted in days rather than as `now - days * 24h`. Those are different questions and only agree
 * in a zone that never shifts its offset: a window spanning a DST change is an hour off, and one
 * asked at 4 p.m. otherwise starts at 4 p.m. on its first day and silently drops that morning.
 */
fun startOfDayDaysAgo(days: Int, clock: Clock = Clock.System, zone: TimeZone = TimeZone.currentSystemDefault()): Long {
    val today = clock.now().toLocalDateTime(zone).date
    return today.minus(days, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds()
}
