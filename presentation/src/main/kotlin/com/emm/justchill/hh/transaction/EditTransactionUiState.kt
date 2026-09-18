package com.emm.justchill.hh.transaction

import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.transaction.Transaction
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.category.SelectableCategory
import com.emm.justchill.core.ui.format.centsToSoles
import com.emm.justchill.core.ui.format.moneyCentsString
import com.emm.justchill.core.ui.format.relativeDayLabel
import com.emm.justchill.core.ui.mvi.UiState
import com.emm.justchill.core.ui.transaction.Catalog
import com.emm.justchill.hh.shared.Empty
import kotlinx.datetime.LocalDate

data class EditTransactionUiState(
    val date: LocalDate,
    val today: LocalDate,
    // The row as stored, and the only yardstick for "was anything edited". Null until it loads.
    val original: Transaction? = null,
    val amount: String = "",
    val description: String = String.Empty,
    val transactionType: TransactionType = TransactionType.Spend,
    val catalog: Catalog = Catalog.Loading,
    val accountId: AccountId? = null,
    val categoryId: CategoryId? = null,
    val frequentCategoryIds: List<String> = emptyList(),
    val openSheet: TransactionSheet? = null,
) : UiState {
    val dateLabel: String get() = relativeDayLabel(date, today)

    val accounts: List<Account> get() = catalog.accounts

    val categories: List<SelectableCategory>
        get() = catalog.loaded?.categories?.get(transactionType.categoryType).orEmpty()

    val accountSelected: Account? get() = accounts.find { it.accountId == accountId }

    // "Uncategorized" is a choice, so there is no fall back to the first row. The stored id is the
    // second candidate rather than the first: a pick of the current type always wins.
    val categorySelected: SelectableCategory?
        get() = categories.find { it.categoryId == categoryId }
            ?: categories.find { it.categoryId == original?.categoryId }

    val missingField: MissingField? get() = when {
        centsToSoles(amount) <= 0.0 -> MissingField.Amount
        accountSelected == null -> MissingField.Account
        else -> null
    }

    val isEnabled: Boolean get() = hasEdits && missingField == null

    private val hasEdits: Boolean
        get() {
            val stored: Transaction = original ?: return false
            return amount != moneyCentsString(stored.amount) ||
                description != stored.description ||
                date != stored.occurredAt.date ||
                transactionType != stored.type ||
                // An unresolvable account leaves accountSelected null, which reads as an edit
                // here but is held shut by missingField; the category has no such second guard.
                accountSelected?.accountId != stored.accountId ||
                categoryEdited(stored.categoryId)
        }

    // Compares the RESOLVED selection: a stored category the catalog can no longer offer is not an
    // edit the user made, or the CTA would be armed the moment such a screen opens.
    private fun categoryEdited(storedId: CategoryId?): Boolean = when {
        storedId == null -> categorySelected != null
        categories.none { it.categoryId == storedId } -> false
        else -> categorySelected?.categoryId != storedId
    }
}
