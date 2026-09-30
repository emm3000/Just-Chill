package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.presentation.date.dateShortcutsOf
import com.emm.justchill.core.presentation.transaction.Catalog
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AddTransactionUiStateTest {

    private val today: LocalDate = LocalDate(2026, Month.AUGUST, 28)

    private val wallet: Account = Account(AccountId("yape"), "Yape")

    @Test
    fun `the shortcuts and the category sections come from the shared rules`() {
        val state: AddTransactionUiState = loadedState(accounts = emptyList()).copy(
            frequentUsage = FrequentUsage(TransactionType.Spend, categoryIds = listOf("coffee", "market")),
        )

        assertEquals(dateShortcutsOf(today), state.dateShortcuts)
        assertEquals(listOf(coffee, market), state.frequentCategories)
        assertEquals(listOf(taxi), state.otherCategories)
    }

    @Test
    fun `a search goes through the shared accent-free match`() {
        assertEquals(listOf(coffee), loadedState(accounts = emptyList()).categoriesMatching("cafe "))
    }

    @Test
    fun `save is enabled once an amount and an account are in place`() {
        assertTrue(loadedState(accounts = listOf(wallet)).copy(amount = "1250").isSaveEnabled)
    }

    @Test
    fun `save stays disabled while a save is in flight`() {
        assertFalse(loadedState(accounts = listOf(wallet)).copy(amount = "1250", isSaving = true).isSaveEnabled)
    }

    @Test
    fun `save stays disabled while a field is missing`() {
        assertFalse(loadedState(accounts = listOf(wallet)).isSaveEnabled)
        assertFalse(loadedState(accounts = emptyList()).copy(amount = "1250").isSaveEnabled)
    }

    private fun loadedState(accounts: List<Account>): AddTransactionUiState = AddTransactionUiState(
        today = today,
        catalog = Catalog.Loaded(
            accounts = accounts,
            categories = mapOf(CategoryType.Spend to listOf(market, taxi, coffee)),
        ),
    )
}
