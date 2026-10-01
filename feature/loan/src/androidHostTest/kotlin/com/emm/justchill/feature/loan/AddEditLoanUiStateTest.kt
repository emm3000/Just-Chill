package com.emm.justchill.feature.loan

import com.emm.justchill.core.presentation.date.dateShortcutsOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Test
import kotlin.test.assertEquals

class AddEditLoanUiStateTest {

    @Test
    fun `the date sheet offers the shortcuts of the state's day`() {
        val today: LocalDate = LocalDate(2026, Month.AUGUST, 28)
        val state: AddEditLoanUiState = AddEditLoanUiState(today = today, date = LocalDate(2026, Month.AUGUST, 10))

        assertEquals(dateShortcutsOf(today), state.dateShortcuts)
    }
}
