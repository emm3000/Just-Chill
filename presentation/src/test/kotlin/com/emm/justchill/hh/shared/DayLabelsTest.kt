package com.emm.justchill.hh.shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals

class DayLabelsTest {

    private val today = LocalDate(2026, Month.AUGUST, 10)

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

    // ── timeLabel ─────────────────────────────────────────────────────────────

    @Test
    fun timeLabel_renders_the_wall_clock_time_it_is_given() {
        // No zone, no conversion: the label shows the hour the movement was recorded at, and it
        // shows the same hour on a phone in Lima and a phone in Karachi. That used to depend on
        // where the device was, because the row carried an instant and this had to guess a zone.
        assertEquals("9:05 a. m.", timeLabel(LocalTime(9, 5)))
        assertEquals("3:45 p. m.", timeLabel(LocalTime(15, 45)))
    }

    @Test
    fun timeLabel_renders_midnight_and_noon_with_the_right_marker() {
        assertEquals("12:00 a. m.", timeLabel(LocalTime(0, 0)))
        assertEquals("12:00 p. m.", timeLabel(LocalTime(12, 0)))
    }

    @Test
    fun timeLabel_ignores_the_seconds_the_column_carries() {
        assertEquals("9:05 a. m.", timeLabel(LocalTime(9, 5, 33)))
    }
}
