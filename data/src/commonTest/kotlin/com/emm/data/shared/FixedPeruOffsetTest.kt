package com.emm.data.shared

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        val january = LocalDateTime(2026, Month.JANUARY, 15, 12, 0, 0)
        val july = LocalDateTime(2026, Month.JULY, 15, 12, 0, 0)

        val januaryUtcHour = (january.toFixedPeruEpochMillis() / 3_600_000) % 24
        val julyUtcHour = (july.toFixedPeruEpochMillis() / 3_600_000) % 24
        assertEquals(januaryUtcHour, julyUtcHour)
    }

    @Test
    fun a_value_outside_the_storable_range_decodes_to_null() {
        assertNull(localDateTimeFromFixedPeru(Long.MAX_VALUE))
        assertNull(localDateTimeFromFixedPeru(Long.MIN_VALUE))
    }

    @Test
    fun no_long_whatsoever_makes_the_conversion_throw() {
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
