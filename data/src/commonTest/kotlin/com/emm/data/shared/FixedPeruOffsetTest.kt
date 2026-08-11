package com.emm.data.shared

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The interim adapter between the app's zoneless `occurredAt` and the epoch millis the sync wire
 * and the v1 backup format still carry.
 *
 * The property that matters is that it is **exactly symmetric**. The bug this whole change removes
 * was two conversions that were supposed to be inverses of each other and quietly stopped being
 * so. This one is pinned to a fixed offset in both directions, which is what makes the round trip
 * lossless — and what makes it correct regardless of where the device is.
 */
class FixedPeruOffsetTest {

    @Test
    fun encodes_at_a_fixed_minus_five_offset() {
        // 2025-07-31 17:13:20 in Lima is 22:13:20 UTC.
        val occurredAt = LocalDateTime(2025, Month.JULY, 31, 17, 13, 20)

        assertEquals(1_754_000_000_000L, occurredAt.toFixedPeruEpochMillis())
    }

    @Test
    fun decodes_at_the_same_fixed_offset() {
        assertEquals(
            LocalDateTime(2025, Month.JULY, 31, 17, 13, 20),
            localDateTimeFromFixedPeru(1_754_000_000_000L),
        )
    }

    @Test
    fun the_round_trip_returns_the_value_unchanged() {
        // Push then pull has to be a no-op, or a synced device rewrites its own rows every cycle.
        listOf(
            LocalDateTime(2026, Month.AUGUST, 10, 0, 0, 0),
            LocalDateTime(2026, Month.AUGUST, 10, 23, 59, 59),
            LocalDateTime(1970, Month.JANUARY, 1, 0, 0, 0),
            LocalDateTime(1969, Month.DECEMBER, 31, 19, 0, 0),
            LocalDateTime(2099, Month.DECEMBER, 31, 12, 34, 56),
        ).forEach { original ->
            assertEquals(original, localDateTimeFromFixedPeru(original.toFixedPeruEpochMillis()))
        }
    }

    @Test
    fun the_offset_does_not_move_across_a_northern_summer() {
        // The whole reason it is fixed rather than `TimeZone.of("America/Lima")`: no DST rule can
        // creep in and make two rows written six months apart disagree by an hour.
        val january = LocalDateTime(2026, Month.JANUARY, 15, 12, 0, 0)
        val july = LocalDateTime(2026, Month.JULY, 15, 12, 0, 0)

        val januaryUtcHour = (january.toFixedPeruEpochMillis() / 3_600_000) % 24
        val julyUtcHour = (july.toFixedPeruEpochMillis() / 3_600_000) % 24
        assertEquals(januaryUtcHour, julyUtcHour)
    }

    @Test
    fun a_value_outside_the_storable_range_decodes_to_null() {
        // A remote row is untrusted input, and this client has already had to survive a server
        // timestamp it could not read. Null lets the caller skip one row instead of failing a pull.
        //
        // What actually happens: `Instant.fromEpochMilliseconds` clamps instead of throwing, so
        // the conversion succeeds and hands back a perfectly real datetime in year 292278994 —
        // which the storage format would write with a signed ten-digit year and which would break
        // the string ordering the whole table depends on. One bad remote row, every list
        // reordered. The year bound is what stops it.
        assertNull(localDateTimeFromFixedPeru(Long.MAX_VALUE))
        assertNull(localDateTimeFromFixedPeru(Long.MIN_VALUE))
    }

    @Test
    fun no_long_whatsoever_makes_the_conversion_throw() {
        // The contract is "returns null", not "returns null for the values we thought of": the
        // call site is outside the pull's per-row try/catch, so a throw does not skip one row, it
        // aborts the whole pull.
        //
        // Run this suite on BOTH targets — `./gradlew :data:testAndroidHostTest` and
        // `./gradlew :data:iosSimulatorArm64Test`. The gate compiles iOS but runs no iOS test, and
        // the year range of a `LocalDateTime` is the kind of thing that has differed per target
        // before. Measured on kotlinx-datetime 0.8.0 both agree, and nothing here says they must
        // keep agreeing — only that whatever they do, the answer is a value or null.
        val values = listOf(
            Long.MIN_VALUE,
            Long.MIN_VALUE + 1,
            Long.MIN_VALUE / 2,
            -1_000_000_000_000_000L,
            -253_402_318_800_000L,
            0L,
            253_402_318_800_000L,
            1_000_000_000_000_000L,
            Long.MAX_VALUE / 2,
            Long.MAX_VALUE - 1,
            Long.MAX_VALUE,
        )

        values.forEach { millis ->
            val decoded = localDateTimeFromFixedPeru(millis)
            assertTrue(
                decoded == null || decoded.year in 1..9999,
                "$millis decoded to $decoded, which the storage format cannot hold",
            )
        }
    }

    @Test
    fun the_edges_of_the_storable_range_still_decode() {
        val lastStorable = LocalDateTime(9999, Month.DECEMBER, 31, 18, 59, 59)

        assertEquals(lastStorable, localDateTimeFromFixedPeru(lastStorable.toFixedPeruEpochMillis()))
    }
}
