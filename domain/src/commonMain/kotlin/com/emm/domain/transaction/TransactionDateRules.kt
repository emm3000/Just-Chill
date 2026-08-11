package com.emm.domain.transaction

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * A transaction records money that already moved, so its day cannot be after today.
 *
 * Nothing enforced this before: the picker navigated forward without a limit and the schema accepts
 * any INTEGER. A transaction dated in 2030 counts in the balance and wins `lastUsedAccountId`,
 * which orders by `date DESC` — so one typo silently changed the account every later screen
 * defaulted to.
 *
 * Both write paths go through here rather than each growing its own copy. The rule belongs to the
 * transaction, not to the act of creating or of editing one.
 *
 * The comparison is between calendar DAYS in [zone], not between instants. Two reasons:
 *  - the app means a day where the column stores an instant (see docs/DATE_AUDIT.md #5), so the day
 *    is the thing the rule is actually about;
 *  - on the Edit path the time of day comes from the ORIGINAL transaction, so an instant comparison
 *    would reject moving a transaction stamped 23:00 while it is 09:00 — same day, not the future.
 */
internal fun ensureNotFutureDated(dateMillis: Long, clock: Clock, zone: TimeZone) {
    val day = Instant.fromEpochMilliseconds(dateMillis).toLocalDateTime(zone).date
    val today = clock.now().toLocalDateTime(zone).date
    if (day > today) {
        throw DomainException.ValidationError(
            "Transaction date $day is after today ($today)",
            ValidationCode.DateInTheFuture,
        )
    }
}
