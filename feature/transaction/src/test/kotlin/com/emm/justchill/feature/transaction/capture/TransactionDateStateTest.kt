package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.domain.transaction.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// The date is the state, and the label is derived from it, so those two can never disagree and
// there is nothing left for a call site to substitute.
class TransactionDateStateTest {

    private val today = LocalDate(2026, Month.AUGUST, 10)

    @Test
    fun add_state_carries_the_selected_day_itself() {
        val state = AddTransactionUiState(today = today, date = LocalDate(2026, Month.MARCH, 4))

        assertEquals(LocalDate(2026, Month.MARCH, 4), state.date)
    }

    @Test
    fun add_state_label_is_derived_from_the_selected_day() {
        val state = AddTransactionUiState(today = today, date = today)

        assertEquals("Hoy", state.dateLabel)
        assertEquals("4 mar", state.copy(date = LocalDate(2026, Month.MARCH, 4)).dateLabel)
        assertEquals("Ayer", state.copy(date = LocalDate(2026, Month.AUGUST, 9)).dateLabel)
    }

    @Test
    fun add_state_label_follows_a_copy_of_the_value() {
        val state = AddTransactionUiState(today = today, date = today)

        assertEquals("Hoy", state.dateLabel)
        assertEquals("13 jun", state.copy(date = LocalDate(2026, Month.JUNE, 13)).dateLabel)
    }

    @Test
    fun add_state_day_is_unset_until_one_is_picked() {
        assertNull(AddTransactionUiState(today = today).date)
    }

    @Test
    fun add_state_reads_an_unset_day_as_Hoy_whatever_today_is() {
        // No clock is consulted for this label, so it cannot go stale: an unset day *is* Hoy, on
        // whichever day the transaction ends up being saved.
        assertEquals("Hoy", AddTransactionUiState(today = today).dateLabel)
        assertEquals("Hoy", AddTransactionUiState(today = LocalDate(2027, Month.JANUARY, 1)).dateLabel)
    }

    @Test
    fun add_state_picked_day_is_labelled_against_today() {
        // A day the user picked is a fact and stays put; only its label moves as today does.
        val picked = AddTransactionUiState(today = today, date = today)

        assertEquals("Hoy", picked.dateLabel)
        assertEquals("Ayer", picked.copy(today = LocalDate(2026, Month.AUGUST, 11)).dateLabel)
        assertEquals(today, picked.copy(today = LocalDate(2026, Month.AUGUST, 11)).date)
    }

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
            AddTransactionUiState(today = today, date = day).dateLabel,
            EditTransactionUiState(date = day, today = today).dateLabel,
        )
    }

    @Test
    fun add_state_defaults_transactionType_to_Spend() {
        assertEquals(TransactionType.Spend, AddTransactionUiState(today = today).transactionType)
    }

    @Test
    fun edit_state_defaults_transactionType_to_Spend() {
        assertEquals(TransactionType.Spend, EditTransactionUiState(date = today, today = today).transactionType)
    }
}
