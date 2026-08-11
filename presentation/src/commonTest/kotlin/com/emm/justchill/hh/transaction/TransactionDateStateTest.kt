package com.emm.justchill.hh.transaction

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The date these two states carry used to be a formatted `String`, with the real value hidden in a
 * `private var` on the ViewModel. Nothing outside the ViewModel could read it, so the date picker
 * was handed `DateUtils.currentDateInMillis()` at both call sites and reopened on today no matter
 * what the user had chosen — on a March transaction it opened on the current month.
 *
 * The value is the state now and the label is derived from it, so those two can no longer disagree
 * and there is nothing left for a call site to substitute.
 */
class TransactionDateStateTest {

    private val today = LocalDate(2026, Month.AUGUST, 10)

    // ── AddTransactionUiState ─────────────────────────────────────────────────

    @Test
    fun add_state_carries_the_selected_day_itself() {
        val state = AddTransactionUiState(date = LocalDate(2026, Month.MARCH, 4), today = today)

        assertEquals(LocalDate(2026, Month.MARCH, 4), state.date)
    }

    @Test
    fun add_state_label_is_derived_from_the_selected_day() {
        val state = AddTransactionUiState(date = today, today = today)

        assertEquals("Hoy", state.dateLabel)
        assertEquals("4 mar", state.copy(date = LocalDate(2026, Month.MARCH, 4)).dateLabel)
        assertEquals("Ayer", state.copy(date = LocalDate(2026, Month.AUGUST, 9)).dateLabel)
    }

    @Test
    fun add_state_label_follows_a_copy_of_the_value() {
        // A stored label would survive this copy unchanged. That is the drift being designed out.
        val state = AddTransactionUiState(date = today, today = today)

        assertEquals("Hoy", state.dateLabel)
        assertEquals("13 jun", state.copy(date = LocalDate(2026, Month.JUNE, 13)).dateLabel)
    }

    // ── EditTransactionUiState ────────────────────────────────────────────────

    @Test
    fun edit_state_carries_the_loaded_day_itself() {
        val state = EditTransactionUiState(date = LocalDate(2026, Month.MARCH, 4), today = today)

        assertEquals(LocalDate(2026, Month.MARCH, 4), state.date)
    }

    @Test
    fun edit_state_label_is_derived_from_the_loaded_day() {
        val state = EditTransactionUiState(date = LocalDate(2026, Month.MARCH, 4), today = today)

        assertEquals("4 mar", state.dateLabel)
        assertEquals("Hoy", state.copy(date = today).dateLabel)
    }

    @Test
    fun both_states_label_the_same_day_the_same_way() {
        val day = LocalDate(2026, Month.SEPTEMBER, 1)

        assertEquals(
            AddTransactionUiState(date = day, today = today).dateLabel,
            EditTransactionUiState(date = day, today = today).dateLabel,
        )
    }
}
