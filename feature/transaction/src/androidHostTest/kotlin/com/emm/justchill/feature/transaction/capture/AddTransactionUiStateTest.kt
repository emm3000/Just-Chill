package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.presentation.transaction.Catalog
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Test
import kotlin.test.assertEquals

class AddTransactionUiStateTest {

    @Test
    fun `the shortcuts and the category sections come from the shared rules`() {
        val today: LocalDate = LocalDate(2026, Month.AUGUST, 28)
        val state: AddTransactionUiState = AddTransactionUiState(
            today = today,
            catalog = Catalog.Loaded(
                accounts = emptyList(),
                categories = mapOf(CategoryType.Spend to listOf(market, taxi, coffee)),
            ),
            frequentUsage = FrequentUsage(TransactionType.Spend, categoryIds = listOf("coffee", "market")),
        )

        assertEquals(dateShortcutsOf(today), state.dateShortcuts)
        assertEquals(listOf(coffee, market), state.frequentCategories)
        assertEquals(listOf(taxi), state.otherCategories)
    }
}
