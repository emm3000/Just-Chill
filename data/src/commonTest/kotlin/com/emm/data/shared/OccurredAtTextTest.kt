package com.emm.data.shared

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OccurredAtTextTest {

    @Test
    fun encodes_as_ISO_local_text() {
        assertEquals(
            "2026-08-10T21:47:33",
            LocalDateTime(2026, Month.AUGUST, 10, 21, 47, 33).toOccurredAtText(),
        )
    }

    @Test
    fun always_writes_the_seconds() {
        assertEquals("2026-08-10T21:47:00", LocalDateTime(2026, Month.AUGUST, 10, 21, 47).toOccurredAtText())
        assertEquals("2026-01-02T03:04:05", LocalDateTime(2026, Month.JANUARY, 2, 3, 4, 5).toOccurredAtText())
    }

    @Test
    fun drops_the_sub_second_precision_the_app_never_means() {
        val withNanos = LocalDateTime(2026, Month.AUGUST, 10, 21, 47, 33, 999_000_000)

        assertEquals("2026-08-10T21:47:33", withNanos.toOccurredAtText())
    }

    @Test
    fun round_trips_exactly() {
        val original = LocalDateTime(2026, Month.AUGUST, 10, 21, 47, 33)

        assertEquals(original, original.toOccurredAtText().toOccurredAtOrNull())
    }

    @Test
    fun string_order_is_chronological_order() {
        val ascending = listOf(
            LocalDateTime(2025, Month.DECEMBER, 31, 23, 59, 59),
            LocalDateTime(2026, Month.JANUARY, 1, 0, 0, 0),
            LocalDateTime(2026, Month.JANUARY, 1, 0, 0, 1),
            LocalDateTime(2026, Month.FEBRUARY, 1, 0, 0, 0),
            LocalDateTime(2026, Month.OCTOBER, 9, 9, 9, 9),
            LocalDateTime(2026, Month.OCTOBER, 10, 0, 0, 0),
        ).map { it.toOccurredAtText() }

        assertEquals(ascending, ascending.sorted())
    }

    @Test
    fun a_bare_day_bound_covers_the_whole_day() {
        val start = "2026-08-01"
        val end = "2026-09-01"

        assertTrue(LocalDateTime(2026, Month.AUGUST, 1, 0, 0, 0).toOccurredAtText() >= start)
        assertTrue(LocalDateTime(2026, Month.AUGUST, 31, 23, 59, 59).toOccurredAtText() < end)
        assertTrue(LocalDateTime(2026, Month.JULY, 31, 23, 59, 59).toOccurredAtText() < start)
        assertTrue(LocalDateTime(2026, Month.SEPTEMBER, 1, 0, 0, 0).toOccurredAtText() >= end)
    }

    @Test
    fun unreadable_text_reads_as_null_rather_than_throwing() {
        assertNull("".toOccurredAtOrNull())
        assertNull("not a date".toOccurredAtOrNull())
        assertNull("1754000000000".toOccurredAtOrNull())
        assertNull("2026-13-45T99:99:99".toOccurredAtOrNull())
    }

    @Test
    fun the_seconds_less_form_still_reads() {
        assertEquals(
            LocalDateTime(2026, Month.AUGUST, 10, 21, 47),
            "2026-08-10T21:47".toOccurredAtOrNull(),
        )
    }
}
