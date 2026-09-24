package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.transaction.FrequentCombo
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.category.SelectableCategory
import com.emm.justchill.core.ui.transaction.Catalog
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Test
import kotlin.test.assertEquals

class AddTransactionCombosTest {

    private val bcp: Account = Account(AccountId("bcp"), "BCP")
    private val yape: Account = Account(AccountId("yape"), "Yape")
    private val closed: Account = Account(AccountId("closed"), "Cuenta cerrada")
    private val market: SelectableCategory = selectableCategory("market", "Supermercado")
    private val taxi: SelectableCategory = selectableCategory("taxi", "Taxi")
    private val coffee: SelectableCategory = selectableCategory("coffee", "Café")

    @Test
    fun `the top ranked combo preselects the account and the category`() {
        val state: AddTransactionUiState = stateWith(listOf(combo(yape, coffee), combo(bcp, market)))

        assertEquals("yape", state.accountSelected?.accountId?.value)
        assertEquals("coffee", state.categorySelected?.categoryId?.value)
    }

    @Test
    fun `a ranked combo naming a deleted account leaves the normal defaults in place`() {
        val state: AddTransactionUiState =
            stateWith(listOf(FrequentCombo(closed.accountId, taxi.categoryId, TransactionType.Spend)))

        assertEquals("bcp", state.accountSelected?.accountId?.value)
        assertEquals("market", state.categorySelected?.categoryId?.value)
    }

    @Test
    fun `a ranked combo naming a deleted category leaves the normal defaults in place`() {
        val state: AddTransactionUiState =
            stateWith(listOf(FrequentCombo(yape.accountId, CategoryId("gone"), TransactionType.Spend)))

        assertEquals("bcp", state.accountSelected?.accountId?.value)
        assertEquals("market", state.categorySelected?.categoryId?.value)
    }

    @Test
    fun `the first combo whose account and category both exist preselects past pruned ones`() {
        val state: AddTransactionUiState = stateWith(
            listOf(
                FrequentCombo(closed.accountId, taxi.categoryId, TransactionType.Spend),
                FrequentCombo(yape.accountId, CategoryId("gone"), TransactionType.Spend),
                FrequentCombo(bcp.accountId, CategoryId("gone"), TransactionType.Spend),
                FrequentCombo(closed.accountId, market.categoryId, TransactionType.Spend),
                combo(yape, coffee),
            ),
        )

        assertEquals("yape", state.accountSelected?.accountId?.value)
        assertEquals("coffee", state.categorySelected?.categoryId?.value)
    }

    @Test
    fun `combos loaded for the other type preselect nothing`() {
        val state: AddTransactionUiState = stateWith(listOf(combo(yape, coffee)))
            .copy(transactionType = TransactionType.Income)

        assertEquals("bcp", state.accountSelected?.accountId?.value)
    }

    @Test
    fun `an explicit pick wins over the ranked combo`() {
        val state: AddTransactionUiState = stateWith(listOf(combo(yape, coffee)))
            .copy(accountId = bcp.accountId, categoryId = taxi.categoryId)

        assertEquals("bcp", state.accountSelected?.accountId?.value)
        assertEquals("taxi", state.categorySelected?.categoryId?.value)
    }

    private fun stateWith(combos: List<FrequentCombo>): AddTransactionUiState = AddTransactionUiState(
        today = LocalDate(2026, Month.AUGUST, 28),
        catalog = Catalog.Loaded(
            accounts = listOf(bcp, yape),
            categories = mapOf(CategoryType.Spend to listOf(market, taxi, coffee)),
        ),
        frequentUsage = FrequentUsage(loadedFor = TransactionType.Spend, categoryIds = emptyList(), combos = combos),
    )

    private fun combo(account: Account, category: SelectableCategory): FrequentCombo =
        FrequentCombo(account.accountId, category.categoryId, TransactionType.Spend)

    private fun selectableCategory(id: String, name: String): SelectableCategory = SelectableCategory(
        categoryId = CategoryId(id),
        name = name,
        iconId = "icon",
        categoryType = CategoryType.Spend,
        colorId = "color",
    )
}
