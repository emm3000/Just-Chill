package com.emm.data.shared

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * The only two places a transaction's occurrence still crosses between local text and an instant,
 * both pinned to a FIXED America/Lima offset rather than to wherever the device happens to be.
 *
 * Two callers, for two different reasons:
 *
 *  1. **The sync wire — interim.** The server still declares `transactions.date bigint not null`.
 *     Changing that column is a human step on a live project and is scheduled as phase two in
 *     `docs/DATE_AUDIT.md`. Until then push encodes and pull decodes here, in one place, so the
 *     conversion cannot drift apart in the two directions. The previous code did this in the
 *     device's *current* zone, which meant the same remote row decoded to a different day
 *     depending on where the phone was — a known defect. **Phase two deletes this caller.**
 *
 *  2. **Reading a v1 backup file — permanent.** Backup files already written to the user's disk
 *     carry epoch millis and are beyond the reach of any migration. They must keep restoring, and
 *     the offset they were written under is this one.
 *
 * A fixed offset makes the round trip exact and symmetric — encode then decode returns the value
 * unchanged — which is what makes it lossless while the wire still speaks millis.
 *
 * The assumption: Peru has had no DST since 1994, and this app's data was written on Lima devices.
 * The same assumption the 3 → 4 schema migration makes, stated the same way.
 */
private val fixedPeruZone: TimeZone = UtcOffset(hours = -5).asTimeZone()

internal fun LocalDateTime.toFixedPeruEpochMillis(): Long = toInstant(fixedPeruZone).toEpochMilliseconds()

/**
 * Null when [epochMillis] cannot become a value this app can store.
 *
 * A remote row is untrusted input; this repository has already had to survive a server timestamp
 * the client could not read (commit `c9492e3`). Null lets the caller skip that one row instead of
 * taking down the whole pull.
 *
 * The year bound is not cosmetic. `Instant.fromEpochMilliseconds` CLAMPS rather than throws, so an
 * absurd value arrives as a real datetime in year 292278994 — which the storage format writes with
 * a signed ten-digit year, and that single row would break the lexicographic ordering the whole
 * table's `ORDER BY` and month windows depend on. A four-digit year is what the format can hold,
 * so anything else is not a date this app can store.
 */
internal fun localDateTimeFromFixedPeru(epochMillis: Long): LocalDateTime? {
    val local = try {
        Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(fixedPeruZone)
    } catch (_: IllegalArgumentException) {
        return null
    }
    return local.takeIf { it.year in STORABLE_YEARS }
}

private val STORABLE_YEARS = 1..9999
