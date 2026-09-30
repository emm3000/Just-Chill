package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.presentation.date.dateShortcutsOf
import com.emm.justchill.core.presentation.transaction.Catalog
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Test
import kotlin.test.assertEquals

class EditTransactionUiStateTest {

    @Test
    fun `the shortcuts and the category sections come from the shared rules`() {
        val state: EditTransactionUiState = stateWithFrequent(listOf("coffee", "market"))

        assertEquals(dateShortcutsOf(state.today), state.dateShortcuts)
        assertEquals(listOf(coffee, market), state.frequentCategories)
        assertEquals(listOf(taxi), state.otherCategories)
    }

    @Test
    fun `a search goes through the shared accent-free match`() {
        val state: EditTransactionUiState = stateWithFrequent(emptyList())

        assertEquals(listOf(coffee), state.categoriesMatching("cafe "))
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
