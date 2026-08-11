package com.emm.domain.shared

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * The inclusive lower bound of a "last N days" window, as an ISO day string (`'2026-05-13'`) —
 * the start of the day [days] calendar days before today in [zone].
 *
 * A bare day compares correctly against `transactions.occurredAt`: every wall-clock time on that
 * day sorts after the bare date, so the whole first day of the window is inside it.
 *
 * Counted in days rather than as `now - days * 24h`. Those are different questions and only agree
 * in a zone that never shifts its offset: a window spanning a DST change is an hour off, and one
 * asked at 4 p.m. otherwise starts at 4 p.m. on its first day and silently drops that morning.
 *
 * [zone] is here to answer one question — what today's local date is. The bound it produces
 * carries no zone at all.
 */
fun startOfDayDaysAgo(
    days: Int,
    clock: Clock = Clock.System,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val today = clock.now().toLocalDateTime(zone).date
    return today.minus(days, DateTimeUnit.DAY).toString()
}
