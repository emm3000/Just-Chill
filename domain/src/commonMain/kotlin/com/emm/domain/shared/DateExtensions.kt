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
 * A bare day compares correctly against `transactions.occurredAt` by the prefix rule: every
 * wall-clock time on that day is the bare date plus more characters, and a string sorts after any
 * prefix of itself — so the whole first day of the window is inside it.
 *
 * Counted in days rather than as `now - days * 24h`. Those are different questions and only agree
 * in a zone that never shifts its offset: a window spanning a DST change is an hour off, and one
 * asked at 4 p.m. otherwise starts at 4 p.m. on its first day and silently drops that morning.
 *
 * [zone] is here to answer one question — what today's local date is. The bound it produces
 * carries no zone at all.
 *
 * Neither [clock] nor [zone] has a default: a window that starts wherever the machine happens to be
 * is a window no test can pin, and a caller that meant to say so can still pass `Clock.System`.
 */
fun startOfDayDaysAgo(days: Int, clock: Clock, zone: TimeZone): String {
    val today = clock.now().toLocalDateTime(zone).date
    return today.minus(days, DateTimeUnit.DAY).toString()
}
