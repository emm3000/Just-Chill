package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.presentation.category.SelectableCategory
import com.emm.justchill.core.presentation.transaction.Catalog
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Test
import kotlin.test.assertEquals

class EditTransactionUiStateTest {

    private val market: SelectableCategory = selectableCategory("market", "Supermercado")
    private val taxi: SelectableCategory = selectableCategory("taxi", "Taxi")
    private val coffee: SelectableCategory = selectableCategory("coffee", "Café")

    @Test
    fun `the date shortcuts name today, yesterday, the week's monday and the month's first day`() {
        val state: EditTransactionUiState = stateWithFrequent(emptyList())

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
    fun `two frequent categories lead in rank order and the rest keep catalog order`() {
        val state: EditTransactionUiState = stateWithFrequent(listOf("coffee", "market"))

        assertEquals(listOf(coffee, market), state.frequentCategories)
        assertEquals(listOf(taxi), state.otherCategories)
    }

    @Test
    fun `a single frequent category makes no section and every category stays in the rest`() {
        val state: EditTransactionUiState = stateWithFrequent(listOf("coffee"))

        assertEquals(emptyList<SelectableCategory>(), state.frequentCategories)
        assertEquals(listOf(market, taxi, coffee), state.otherCategories)
    }

    @Test
    fun `a search matches names ignoring case and surrounding spaces`() {
        val state: EditTransactionUiState = stateWithFrequent(emptyList())

        assertEquals(listOf(market), state.categoriesMatching("  SUPER "))
        assertEquals(listOf(coffee), state.categoriesMatching("caf"))
    }

    @Test
    fun `a blank search matches every category in catalog order`() {
        val state: EditTransactionUiState = stateWithFrequent(emptyList())

        assertEquals(listOf(market, taxi, coffee), state.categoriesMatching("   "))
    }

    @Test
    fun `the note is the description the state carries`() {
        val state: EditTransactionUiState = stateWithFrequent(emptyList()).copy(description = "Mercado del lunes")

        assertEquals("Mercado del lunes", state.note)
    }

    private fun stateWithFrequent(categoryIds: List<String>): EditTransactionUiState = EditTransactionUiState(
        date = LocalDate(2026, Month.AUGUST, 10),
        today = LocalDate(2026, Month.AUGUST, 28),
        catalog = Catalog.Loaded(
            accounts = emptyList(),
            categories = mapOf(CategoryType.Spend to listOf(market, taxi, coffee)),
        ),
        frequentCategoryIds = categoryIds,
    )
}
