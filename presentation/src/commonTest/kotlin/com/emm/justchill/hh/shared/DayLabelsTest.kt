package com.emm.justchill.hh.shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class DayLabelsTest {

    /** UTC-5. Local evenings fall on the next UTC day. */
    private val lima = TimeZone.of("America/Lima")

    /** UTC+5. Local midnight falls on the previous UTC day. */
    private val karachi = TimeZone.of("Asia/Karachi")

    private val today = LocalDate(2026, Month.AUGUST, 10)

    private fun LocalDate.at(hour: Int, minute: Int, zone: TimeZone): Long =
        (atStartOfDayIn(zone) + hour.hours + minute.minutes).toEpochMilliseconds()

    // ── relativeDayLabel ──────────────────────────────────────────────────────

    @Test
    fun today_is_Hoy() {
        assertEquals("Hoy", relativeDayLabel(today, today))
    }

    @Test
    fun the_day_before_is_Ayer() {
        assertEquals("Ayer", relativeDayLabel(LocalDate(2026, Month.AUGUST, 9), today))
    }

    @Test
    fun the_day_after_is_Manana() {
        assertEquals("Mañana", relativeDayLabel(LocalDate(2026, Month.AUGUST, 11), today))
    }

    @Test
    fun any_other_day_is_the_short_date() {
        assertEquals("13 jun", relativeDayLabel(LocalDate(2026, Month.JUNE, 13), today))
        assertEquals("1 sept", relativeDayLabel(LocalDate(2026, Month.SEPTEMBER, 1), today))
    }

    @Test
    fun the_relative_branch_crosses_a_year_boundary() {
        val newYearsDay = LocalDate(2027, Month.JANUARY, 1)
        assertEquals("Ayer", relativeDayLabel(LocalDate(2026, Month.DECEMBER, 31), newYearsDay))
        assertEquals("Mañana", relativeDayLabel(LocalDate(2027, Month.JANUARY, 2), newYearsDay))
    }

    @Test
    fun the_label_moves_only_when_today_moves() {
        // The whole reason `today` is a parameter: every row in one mapping pass agrees, and the
        // caller — not this function — decides when "Hoy" rolls over.
        val date = LocalDate(2026, Month.AUGUST, 10)
        assertEquals("Hoy", relativeDayLabel(date, today))
        assertEquals("Ayer", relativeDayLabel(date, LocalDate(2026, Month.AUGUST, 11)))
    }

    // ── localDateOf ───────────────────────────────────────────────────────────

    @Test
    fun localDateOf_resolves_the_day_in_the_given_zone() {
        // 23:30 in Lima is already the 11th in UTC. Reading the day in UTC is the bug this guards.
        val limaEvening = today.at(hour = 23, minute = 30, zone = lima)

        assertEquals(LocalDate(2026, Month.AUGUST, 10), localDateOf(limaEvening, lima))
        assertEquals(LocalDate(2026, Month.AUGUST, 11), localDateOf(limaEvening, TimeZone.UTC))
    }

    @Test
    fun localDateOf_resolves_the_day_in_zones_ahead_of_UTC() {
        val karachiMidnight = today.atStartOfDayIn(karachi).toEpochMilliseconds()

        assertEquals(LocalDate(2026, Month.AUGUST, 10), localDateOf(karachiMidnight, karachi))
        assertEquals(LocalDate(2026, Month.AUGUST, 9), localDateOf(karachiMidnight, TimeZone.UTC))
    }

    // ── timeLabel ─────────────────────────────────────────────────────────────

    @Test
    fun timeLabel_renders_the_clock_time_of_the_given_zone() {
        assertEquals("9:05 a. m.", timeLabel(today.at(hour = 9, minute = 5, zone = lima), lima))
        assertEquals("3:45 p. m.", timeLabel(today.at(hour = 15, minute = 45, zone = lima), lima))
    }

    @Test
    fun timeLabel_renders_midnight_and_noon_with_the_right_marker() {
        assertEquals("12:00 a. m.", timeLabel(today.at(hour = 0, minute = 0, zone = lima), lima))
        assertEquals("12:00 p. m.", timeLabel(today.at(hour = 12, minute = 0, zone = lima), lima))
    }

    // ── startOfDayMillis ──────────────────────────────────────────────────────

    @Test
    fun startOfDayMillis_is_local_midnight_not_UTC_midnight() {
        assertEquals(today.atStartOfDayIn(lima).toEpochMilliseconds(), startOfDayMillis(today, lima))
        assertEquals(today, localDateOf(startOfDayMillis(today, lima), lima))
        assertEquals(today, localDateOf(startOfDayMillis(today, karachi), karachi))
    }
}
