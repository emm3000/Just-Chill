package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.transaction.FrequentCombo
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.domain.transaction.amountBandKey
import com.emm.justchill.core.ui.category.SelectableCategory
import com.emm.justchill.core.ui.format.centsToMoney
import com.emm.justchill.core.ui.format.centsToSoles
import com.emm.justchill.core.ui.format.relativeDayLabel
import com.emm.justchill.core.ui.mvi.UiState
import com.emm.justchill.core.ui.transaction.Catalog
import com.emm.justchill.feature.transaction.capture.comboLabel
import kotlinx.datetime.LocalDate

data class FrequentUsage(
    val loadedFor: TransactionType,
    val categoryIds: List<String>,
    val combos: List<FrequentCombo> = emptyList(),
    val loadedForAmountBand: Long = 0L,
)

data class AddTransactionUiState(
    val today: LocalDate,
    // null is not "no date" — it is the day this gets saved on, staying unresolved until save.
    val date: LocalDate? = null,
    val amount: String = "",
    val description: String = "",
    val transactionType: TransactionType = TransactionType.Spend,
    val isSaving: Boolean = false,
    val catalog: Catalog = Catalog.Loading,
    val lastUsedAccountId: AccountId? = null,
    val accountId: AccountId? = null,
    val categoryId: CategoryId? = null,
    // Bridges the frame between creating a category here and the repository flow re-emitting with
    // it. The catalog's row supersedes it by id, so this can never serve a stale copy.
    val extraCategories: List<SelectableCategory> = emptyList(),
    val frequentUsage: FrequentUsage? = null,
    // nav3 disposes and recreates this screen's composition on events the ViewModel survives —
    // rotation, and popping back from CategoryRoute in particular — which re-sends the same
    // preselect. Only the first registration may act; every repeat is a no-op.
    val preselectConsumed: Boolean = false,
    val openSheet: TransactionSheet? = null,
) : UiState {
    val dateLabel: String get() = date?.let { relativeDayLabel(it, today) } ?: "Hoy"

    val pickerDate: LocalDate get() = date ?: today

    val accounts: List<Account> get() = catalog.accounts

    val hasNoAccounts: Boolean get() = catalog.loaded?.accounts?.isEmpty() == true

    val categories: List<SelectableCategory> get() = categoriesOf(transactionType.categoryType)

    val accountSelected: Account?
        get() = accounts.find { it.accountId == accountId }
            ?: rankedCombo?.let { ranked -> accounts.find { it.accountId.value == ranked.accountId } }
            ?: accounts.find { it.accountId == lastUsedAccountId }
            ?: accounts.firstOrNull()

    val categorySelected: SelectableCategory?
        get() = categories.find { it.categoryId == categoryId }
            ?: rankedCombo?.let { ranked -> categories.find { it.categoryId.value == ranked.categoryId } }
            ?: categories.firstOrNull()

    val frequentCategoryIds: List<String> get() = usageForCurrentType?.categoryIds.orEmpty()

    val frequentCombos: List<FrequentComboUi>
        get() {
            val candidates = categories
            return usageForCurrentType?.combos.orEmpty()
                .mapNotNull { toComboUi(it, candidates) }
                .take(FREQUENT_COMBO_CAP)
        }

    val missingField: MissingField? get() = when {
        centsToSoles(amount) <= 0.0 -> MissingField.Amount
        accountSelected == null -> MissingField.Account
        else -> null
    }

    // The chip row may never render the previous type's or the previous amount band's suggestions,
    // not even for the frame between the change and the reads that answer it.
    private val usageForCurrentType: FrequentUsage?
        get() = frequentUsage?.takeIf {
            it.loadedFor == transactionType && it.loadedForAmountBand == currentAmountBand
        }

    private val currentAmountBand: Long get() = amountBandKey(centsToMoney(amount))

    private val rankedCombo: FrequentComboUi? get() = frequentCombos.firstOrNull()

    private fun categoriesOf(type: CategoryType): List<SelectableCategory> {
        val known = catalog.loaded?.categories?.get(type).orEmpty()
        val pending = extraCategories.filter { extra ->
            extra.categoryType == type && known.none { it.categoryId == extra.categoryId }
        }
        return if (pending.isEmpty()) known else pending + known
    }

    private fun toComboUi(combo: FrequentCombo, candidates: List<SelectableCategory>): FrequentComboUi? {
        val account = accounts.find { it.accountId == combo.accountId }
        val category = candidates.find { it.categoryId == combo.categoryId }
        if (account == null || category == null) return null
        return FrequentComboUi(
            accountId = combo.accountId.value,
            categoryId = combo.categoryId.value,
            type = combo.type,
            label = comboLabel(account.name, category.name),
            colorId = category.colorId,
        )
    }

    private companion object {
        // The use case resolves five so that pruning a deleted account or category cannot empty the
        // row; the form offers the three most-used of what survives, never a browsable list.
        const val FREQUENT_COMBO_CAP = 3
    }
}

enum class MissingField { Amount, Account }
