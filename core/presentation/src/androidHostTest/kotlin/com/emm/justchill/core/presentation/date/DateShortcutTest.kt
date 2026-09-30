package com.emm.justchill.core.presentation.date

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DateShortcutTest {

    private val today: LocalDate = LocalDate(2026, Month.AUGUST, 28)

    @Test
    fun `the date shortcuts name today, yesterday, the week's monday and the month's first day`() {
        assertEquals(
            listOf(
                DateShortcut(DateShortcutKind.Today, LocalDate(2026, Month.AUGUST, 28)),
                DateShortcut(DateShortcutKind.Yesterday, LocalDate(2026, Month.AUGUST, 27)),
                DateShortcut(DateShortcutKind.ThisWeek, LocalDate(2026, Month.AUGUST, 24)),
                DateShortcut(DateShortcutKind.ThisMonth, LocalDate(2026, Month.AUGUST, 1)),
            ),
            dateShortcutsOf(today),
        )
    }

    @Test
    fun `on a monday the week shortcut is today and yesterday falls in the week before`() {
        assertEquals(
            listOf(
                DateShortcut(DateShortcutKind.Today, LocalDate(2026, Month.JUNE, 1)),
                DateShortcut(DateShortcutKind.Yesterday, LocalDate(2026, Month.MAY, 31)),
                DateShortcut(DateShortcutKind.ThisWeek, LocalDate(2026, Month.JUNE, 1)),
                DateShortcut(DateShortcutKind.ThisMonth, LocalDate(2026, Month.JUNE, 1)),
            ),
            dateShortcutsOf(LocalDate(2026, Month.JUNE, 1)),
        )
    }

    @Test
    fun `each shortcut reads its spanish label in order`() {
        assertEquals(
            listOf("Hoy", "Ayer", "Esta semana", "Este mes"),
            dateShortcutsOf(today).map { it.label },
        )
    }

    @Test
    fun `the today pill is active only on its own date`() {
        val todayShortcut: DateShortcut = DateShortcut(DateShortcutKind.Today, today)

        assertTrue(todayShortcut.isActiveFor(today))
        assertFalse(todayShortcut.isActiveFor(LocalDate(2026, Month.AUGUST, 27)))
    }

    @Test
    fun `no other pill is ever active, even on the day it names`() {
        val monday: LocalDate = LocalDate(2026, Month.JUNE, 1)

        assertEquals(
            listOf(true, false, false, false),
            dateShortcutsOf(monday).map { it.isActiveFor(monday) },
        )
    }
}
