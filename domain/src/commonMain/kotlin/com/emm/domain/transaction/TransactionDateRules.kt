package com.emm.domain.transaction

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * A transaction records money that already moved, so its day cannot be after today.
 *
 * Nothing enforced this before: the picker navigated forward without a limit and the schema accepts
 * any value. A transaction dated in 2030 counts in the balance and wins `lastUsedAccountId`, which
 * orders by `occurredAt DESC` — so one typo silently changed the account every later screen
 * defaulted to.
 *
 * Both write paths go through here rather than each growing its own copy. The rule belongs to the
 * transaction, not to the act of creating or of editing one.
 *
 * The comparison is between calendar DAYS, not between instants: on the Edit path the time of day
 * comes from the ORIGINAL transaction, so comparing the full value would reject moving a
 * transaction stamped 23:00 while it is 09:00 — same day, not the future.
 *
 * [clock] and [zone] are read for one thing only: to ask what today's local date is for this user.
 * That question has no answer without a zone, and it is the ONE place the whole date model still
 * needs one — the stored value itself carries none.
 */
internal fun ensureNotFutureDated(occurredAt: LocalDateTime, clock: Clock, zone: TimeZone) {
    val today = clock.now().toLocalDateTime(zone).date
    if (occurredAt.date > today) {
        throw DomainException.ValidationError(
            "Transaction date ${occurredAt.date} is after today ($today)",
            ValidationCode.DateInTheFuture,
        )
    }
}
