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
 * Peru has had no DST since 1994 and this data was written on Lima devices, so a fixed offset —
 * never the device's zone — is what makes the epoch-millis round trip exact. v1 backup files
 * already on a user's disk were written under this offset and must keep restoring.
 */
private val fixedPeruZone: TimeZone = UtcOffset(hours = -5).asTimeZone()

internal fun LocalDateTime.toFixedPeruEpochMillis(): Long = toInstant(fixedPeruZone).toEpochMilliseconds()

/**
 * Both catches are required: DateTimeArithmeticException is a plain RuntimeException, not an
 * IllegalArgumentException. Callers skip a row on null; a throw would abort their whole pass.
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

/**
 * Instant.fromEpochMilliseconds clamps instead of throwing, so Long.MAX_VALUE arrives as a real
 * datetime in year 292278994. Storage text would write that as a signed ten-digit year, breaking
 * the lexicographic ordering every ORDER BY and month window depends on.
 */
private val STORABLE_YEARS = 1..9999
