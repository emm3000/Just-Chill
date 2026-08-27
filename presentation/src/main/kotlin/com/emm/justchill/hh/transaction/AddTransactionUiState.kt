package com.emm.justchill.hh.transaction

import com.emm.domain.account.Account
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.transaction.FrequentCombo
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.shared.Empty
import com.emm.justchill.hh.shared.comboLabel
import com.emm.justchill.hh.shared.relativeDayLabel
import kotlinx.datetime.LocalDate

data class FrequentUsage(
    val loadedFor: TransactionType,
    val categoryIds: List<String>,
    val combos: List<FrequentCombo> = emptyList(),
)

data class AddTransactionUiState(
    val today: LocalDate,
    /**
     * The day the transaction is recorded for, or `null` while unpicked. `null` is not "no date" —
     * it is *the day this gets saved on*, staying unresolved until the save happens.
     */
    val date: LocalDate? = null,
    val amount: String = "",
    val description: String = String.Empty,
    val transactionType: TransactionType = TransactionType.Spend,
    val isSaving: Boolean = false,
    val catalog: Catalog = Catalog.Loading,
    val lastUsedAccountId: AccountId? = null,
    val accountId: AccountId? = null,
    val categoryId: CategoryId? = null,
    /**
     * Bridges the frame between creating a category here and the repository flow re-emitting with
     * it. The catalog's row supersedes it by id, so this can never serve a stale copy.
     */
    val extraCategories: List<SelectableCategory> = emptyList(),
    val frequentUsage: FrequentUsage? = null,
    /**
     * nav3 disposes and recreates this screen's composition on events the ViewModel survives —
     * rotation, and popping back from CategoryRoute in particular — which re-sends the same
     * preselect. Only the first registration may act; every repeat is a no-op.
     */
    val preselectConsumed: Boolean = false,
) : UiState {
    val dateLabel: String get() = date?.let { relativeDayLabel(it, today) } ?: "Hoy"

    val pickerDate: LocalDate get() = date ?: today

    val accounts: List<Account> get() = catalog.accounts

    val hasNoAccounts: Boolean get() = catalog.loaded?.accounts?.isEmpty() == true

    val categories: List<SelectableCategory> get() = categoriesOf(transactionType.categoryType)

    val accountSelected: Account?
        get() = accounts.find { it.accountId == accountId }
            ?: accounts.find { it.accountId == lastUsedAccountId }
            ?: accounts.firstOrNull()

    val categorySelected: SelectableCategory?
        get() = categories.find { it.categoryId == categoryId } ?: categories.firstOrNull()

    val frequentCategoryIds: List<String> get() = usageForCurrentType?.categoryIds.orEmpty()

    val frequentCombos: List<FrequentComboUi>
        get() {
            val candidates = categories
            return usageForCurrentType?.combos.orEmpty().mapNotNull { toComboUi(it, candidates) }
        }

    val missingField: MissingField? get() = when {
        centsToSoles(amount) <= 0.0 -> MissingField.Amount
        accountSelected == null -> MissingField.Account
        else -> null
    }

    // The chip row may never render the previous type's suggestions, not even for the frame between
    // a type switch and the reads that answer it.
    private val usageForCurrentType: FrequentUsage? get() = frequentUsage?.takeIf { it.loadedFor == transactionType }

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
}

enum class MissingField { Amount, Account }
