package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.presentation.category.SelectableCategory
import com.emm.justchill.core.presentation.transaction.Catalog
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Test
import kotlin.test.assertEquals

class AddTransactionUiStateTest {

    private val market: SelectableCategory = selectableCategory("market", "Supermercado")
    private val taxi: SelectableCategory = selectableCategory("taxi", "Taxi")
    private val coffee: SelectableCategory = selectableCategory("coffee", "Café")

    @Test
    fun `two frequent categories lead in rank order and the rest keep catalog order`() {
        val state: AddTransactionUiState = stateWithFrequent(listOf("coffee", "market"))

        assertEquals(listOf(coffee, market), state.frequentCategories)
        assertEquals(listOf(taxi), state.otherCategories)
    }

    @Test
    fun `a single frequent category makes no section and every category stays in the rest`() {
        val state: AddTransactionUiState = stateWithFrequent(listOf("coffee"))

        assertEquals(emptyList<SelectableCategory>(), state.frequentCategories)
        assertEquals(listOf(market, taxi, coffee), state.otherCategories)
    }

    @Test
    fun `a frequent id naming a deleted category is skipped before the section is counted`() {
        val state: AddTransactionUiState = stateWithFrequent(listOf("gone", "taxi"))

        assertEquals(emptyList<SelectableCategory>(), state.frequentCategories)
        assertEquals(listOf(market, taxi, coffee), state.otherCategories)
    }

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

    private fun stateWithFrequent(categoryIds: List<String>): AddTransactionUiState = AddTransactionUiState(
        today = LocalDate(2026, Month.AUGUST, 28),
        catalog = Catalog.Loaded(
            accounts = emptyList(),
            categories = mapOf(CategoryType.Spend to listOf(market, taxi, coffee)),
        ),
        frequentUsage = FrequentUsage(loadedFor = TransactionType.Spend, categoryIds = categoryIds),
    )

    private fun selectableCategory(id: String, name: String): SelectableCategory = SelectableCategory(
        categoryId = CategoryId(id),
        name = name,
        iconId = "icon",
        categoryType = CategoryType.Spend,
        colorId = "color",
    )
}
