package com.emm.justchill.feature.transaction.capture

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Test
import kotlin.test.assertEquals

class AddTransactionUiStateTest {

    @Test
    fun `the date shortcuts name today, yesterday, the week's monday and the month's first day`() {
        val state: AddTransactionUiState = AddTransactionUiState(today = LocalDate(2026, Month.AUGUST, 28))

        assertEquals(
            listOf(
                DateShortcut(DateShortcutKind.Today, LocalDate(2026, Month.AUGUST, 28)),
                DateShortcut(DateShortcutKind.Yesterday, LocalDate(2026, Month.AUGUST, 27)),
                DateShortcut(DateShortcutKind.ThisWeek, LocalDate(2026, Month.AUGUST, 24)),
                DateShortcut(DateShortcutKind.ThisMonth, LocalDate(2026, Month.AUGUST, 1)),
            ),
            state.dateShortcuts,
        )
    }

    @Test
    fun `on a monday the week shortcut is today and yesterday falls in the week before`() {
        val state: AddTransactionUiState = AddTransactionUiState(today = LocalDate(2026, Month.JUNE, 1))

        assertEquals(
            listOf(
                DateShortcut(DateShortcutKind.Today, LocalDate(2026, Month.JUNE, 1)),
                DateShortcut(DateShortcutKind.Yesterday, LocalDate(2026, Month.MAY, 31)),
                DateShortcut(DateShortcutKind.ThisWeek, LocalDate(2026, Month.JUNE, 1)),
                DateShortcut(DateShortcutKind.ThisMonth, LocalDate(2026, Month.JUNE, 1)),
            ),
            state.dateShortcuts,
        )
    }
}
