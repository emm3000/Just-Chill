package com.emm.justchill.hh.transaction

import com.emm.domain.account.Account
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.transaction.FrequentCombo
import com.emm.domain.transaction.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Test
import kotlin.test.assertEquals

class AddTransactionCombosTest {

    private val bcp = Account(AccountId("bcp"), "BCP")
    private val yape = Account(AccountId("yape"), "Yape")
    private val market = selectableCategory("market", "Supermercado")
    private val taxi = selectableCategory("taxi", "Taxi")
    private val coffee = selectableCategory("coffee", "Café")

    @Test
    fun `the chip row is offered at most three combos`() {
        val state = stateWith(
            listOf(
                combo(bcp, market),
                combo(bcp, taxi),
                combo(bcp, coffee),
                combo(yape, market),
                combo(yape, taxi),
            ),
        )

        assertEquals(3, state.frequentCombos.size)
    }

    @Test
    fun `capping keeps the most-used-first order the use case returned`() {
        val state = stateWith(
            listOf(
                combo(yape, coffee),
                combo(bcp, market),
                combo(bcp, taxi),
                combo(yape, market),
            ),
        )

        assertEquals(
            listOf("Yape · Café", "BCP · Supermercado", "BCP · Taxi"),
            state.frequentCombos.map { it.label },
        )
    }

    @Test
    fun `fewer combos than the cap are offered as-is`() {
        val state = stateWith(listOf(combo(bcp, market)))

        assertEquals(listOf("BCP · Supermercado"), state.frequentCombos.map { it.label })
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
