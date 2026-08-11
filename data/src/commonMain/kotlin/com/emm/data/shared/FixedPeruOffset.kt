package com.emm.data.shared

import kotlinx.datetime.DateTimeArithmeticException
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
 * Null when [epochMillis] cannot become a value this app can store. Total on every target: it
 * returns, it never throws.
 *
 * A remote row is untrusted input; this repository has already had to survive a server timestamp
 * the client could not read (commit `c9492e3`). Null lets the caller skip that one row instead of
 * taking down the whole pull.
 *
 * Two different things can go wrong, and BOTH have to end in null:
 *
 *  - **The year is one the storage format cannot hold.** This is the case that actually fires.
 *    `Instant.fromEpochMilliseconds` clamps rather than throws, so `Long.MAX_VALUE` arrives as a
 *    perfectly real datetime in year 292278994 — which the storage format would write with a signed
 *    ten-digit year, and that single row would break the lexicographic ordering the whole table's
 *    `ORDER BY` and month windows depend on. A four-digit year is what the format can hold, so
 *    anything else is not a date this app can store. The year bound is what catches it, on every
 *    target: measured on kotlinx-datetime 0.8.0, the widest year any `Long` of millis can reach is
 *    ±292278994, and both the JVM and the Apple targets convert that without complaint.
 *  - **The conversion refuses the value outright.** Not reachable from a `Long` today — see above,
 *    the whole millis range converts — but `Instant.toLocalDateTime` is documented to signal it,
 *    and it signals it with [DateTimeArithmeticException]. That is a plain `RuntimeException`, NOT
 *    an [IllegalArgumentException], so a guard that named only the latter would not have caught it.
 *    Both types are named because this function's contract is "returns null", not "returns null on
 *    the ranges we happened to measure": the call site is OUTSIDE `applyRemoteRow`'s try/catch, so
 *    a throw here does not skip a row, it aborts the entire pull.
 */
internal fun localDateTimeFromFixedPeru(epochMillis: Long): LocalDateTime? {
    val local = try {
        Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(fixedPeruZone)
    } catch (_: DateTimeArithmeticException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
    return local?.takeIf { it.year in STORABLE_YEARS }
}

private val STORABLE_YEARS = 1..9999
