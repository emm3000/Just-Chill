package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.account.AccountType
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.domain.transaction.FrequentCombo
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.category.AppIconCatalog
import com.emm.justchill.core.ui.category.SelectableCategory
import com.emm.justchill.core.ui.category.selectableColorIds
import com.emm.justchill.core.ui.transaction.Catalog
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

private const val PREVIEW_YEAR: Int = 2026
private const val PREVIEW_DAY: Int = 23
private const val MONTH_SPEND_CENTS: Long = 132_860L

// ADR 020: two full-width combo rows keep the Redmi 15C tall at 728dp; a third row comes out of the hero.
private const val TALL_BUDGET_COMBO_ROWS: Int = 2

internal fun populatedCaptureState(): AddTransactionUiState {
    val wallet: Account = Account(accountId = AccountId("betsy"), name = "Betsy", type = AccountType.Wallet)
    val bank: Account = Account(accountId = AccountId("bcp"), name = "BCP", type = AccountType.Bank)
    val categories: List<SelectableCategory> = listOf("Gasto diario", "Comida", "Transporte")
        .mapIndexed { index, name ->
            SelectableCategory(
                categoryId = CategoryId(name),
                name = name,
                iconId = AppIconCatalog.catalog[index].id,
                colorId = selectableColorIds[index + 1],
                categoryType = CategoryType.Spend,
            )
        }
    val combos: List<FrequentCombo> = listOf(
        FrequentCombo(wallet.accountId, categories[0].categoryId, TransactionType.Spend),
        FrequentCombo(bank.accountId, categories[1].categoryId, TransactionType.Spend),
        FrequentCombo(wallet.accountId, categories[2].categoryId, TransactionType.Spend),
    )
    return AddTransactionUiState(
        today = LocalDate(PREVIEW_YEAR, Month.SEPTEMBER, PREVIEW_DAY),
        catalog = Catalog.Loaded(
            accounts = listOf(wallet, bank),
            categories = mapOf(CategoryType.Spend to categories),
        ),
        transactionType = TransactionType.Spend,
        frequentUsage = FrequentUsage(
            loadedFor = TransactionType.Spend,
            categoryIds = categories.map { category -> category.categoryId.value },
            combos = combos,
        ),
        monthSpend = MonthSpend(YearMonth(PREVIEW_YEAR, Month.SEPTEMBER), Money(MONTH_SPEND_CENTS)),
    )
}

internal fun noAccountsCaptureState(): AddTransactionUiState {
    val populated: AddTransactionUiState = populatedCaptureState()
    return populated.copy(
        catalog = Catalog.Loaded(
            accounts = emptyList(),
            categories = mapOf(CategoryType.Spend to populated.categories),
        ),
        frequentUsage = FrequentUsage(
            loadedFor = TransactionType.Spend,
            categoryIds = emptyList(),
            combos = emptyList(),
        ),
    )
}

internal fun tallBudgetCaptureState(): AddTransactionUiState {
    val account: Account = Account(
        accountId = AccountId("budget"),
        name = "Cuenta de ahorros principal",
        type = AccountType.Bank,
    )
    val categories: List<SelectableCategory> = List(TALL_BUDGET_COMBO_ROWS) { index: Int ->
        SelectableCategory(
            categoryId = CategoryId("budget-$index"),
            name = "Servicios del hogar y del mes $index",
            iconId = AppIconCatalog.catalog[index].id,
            colorId = selectableColorIds[index],
            categoryType = CategoryType.Spend,
        )
    }
    return AddTransactionUiState(
        today = LocalDate(PREVIEW_YEAR, Month.SEPTEMBER, PREVIEW_DAY),
        catalog = Catalog.Loaded(
            accounts = listOf(account),
            categories = mapOf(CategoryType.Spend to categories),
        ),
        transactionType = TransactionType.Spend,
        frequentUsage = FrequentUsage(
            loadedFor = TransactionType.Spend,
            categoryIds = categories.map { category: SelectableCategory -> category.categoryId.value },
            combos = categories.map { category: SelectableCategory ->
                FrequentCombo(account.accountId, category.categoryId, TransactionType.Spend)
            },
        ),
    )
}
