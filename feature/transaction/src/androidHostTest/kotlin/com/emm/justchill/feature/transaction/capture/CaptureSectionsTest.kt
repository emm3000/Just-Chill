package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.presentation.category.SelectableCategory
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Test
import kotlin.test.assertEquals

class CaptureSectionsTest {

    private val categories: List<SelectableCategory> = listOf(market, taxi, coffee)

    @Test
    fun `the date shortcuts name today, yesterday, the week's monday and the month's first day`() {
        assertEquals(
            listOf(
                DateShortcut(DateShortcutKind.Today, LocalDate(2026, Month.AUGUST, 28)),
                DateShortcut(DateShortcutKind.Yesterday, LocalDate(2026, Month.AUGUST, 27)),
                DateShortcut(DateShortcutKind.ThisWeek, LocalDate(2026, Month.AUGUST, 24)),
                DateShortcut(DateShortcutKind.ThisMonth, LocalDate(2026, Month.AUGUST, 1)),
            ),
            dateShortcutsOf(LocalDate(2026, Month.AUGUST, 28)),
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
    fun `two frequent categories lead in rank order and the rest keep catalog order`() {
        val frequent: List<SelectableCategory> = frequentSectionOf(categories, listOf("coffee", "market"))

        assertEquals(listOf(coffee, market), frequent)
        assertEquals(listOf(taxi), otherSectionOf(categories, frequent))
    }

    @Test
    fun `a single frequent category makes no section and every category stays in the rest`() {
        val frequent: List<SelectableCategory> = frequentSectionOf(categories, listOf("coffee"))

        assertEquals(emptyList<SelectableCategory>(), frequent)
        assertEquals(categories, otherSectionOf(categories, frequent))
    }

    @Test
    fun `a frequent id naming a deleted category is skipped before the section is counted`() {
        val frequent: List<SelectableCategory> = frequentSectionOf(categories, listOf("gone", "taxi"))

        assertEquals(emptyList<SelectableCategory>(), frequent)
        assertEquals(categories, otherSectionOf(categories, frequent))
    }
}
