package com.emm.justchill.hh.transaction

import com.emm.domain.account.Account
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.shared.Empty
import com.emm.justchill.hh.shared.relativeDayLabel
import kotlinx.datetime.LocalDate

data class EditTransactionUiState(
    val date: LocalDate,
    val today: LocalDate,
    /** The row as stored, and the only yardstick for "was anything edited". Null until it loads. */
    val original: Transaction? = null,
    val amount: String = "",
    val description: String = String.Empty,
    val transactionType: TransactionType = TransactionType.Spend,
    val catalog: Catalog = Catalog.Loading,
    val accountId: AccountId? = null,
    val categoryId: CategoryId? = null,
    val frequentCategoryIds: List<String> = emptyList(),
) : UiState {
    val dateLabel: String get() = relativeDayLabel(date, today)

    val accounts: List<Account> get() = loadedCatalog?.accounts.orEmpty()

    val categories: List<SelectableCategory>
        get() = loadedCatalog?.categories?.get(transactionType.categoryType).orEmpty()

    val accountSelected: Account? get() = accounts.find { it.accountId == accountId }

    /**
     * "Uncategorized" is a choice, so there is no fall back to the first row. The stored id is the
     * second candidate rather than the first: a pick of the current type always wins, and switching
     * type away and back re-offers what the movement was actually filed under.
     */
    val categorySelected: SelectableCategory?
        get() = categories.find { it.categoryId == categoryId }
            ?: categories.find { it.categoryId == original?.categoryId }

    val missingField: MissingField? get() = when {
        centsToSoles(amount) <= 0.0 -> MissingField.Amount
        accountSelected == null -> MissingField.Account
        else -> null
    }

    val isEnabled: Boolean get() = hasEdits && missingField == null

    private val loadedCatalog: Catalog.Loaded? get() = catalog as? Catalog.Loaded

    // Compares the RESOLVED selection, not the raw id: an id pointing at a row the catalog no
    // longer carries is not an edit the user made, and saving it would file the movement under a
    // category that is gone.
    private val hasEdits: Boolean
        get() {
            val stored: Transaction = original ?: return false
            return amount != moneyCentsString(stored.amount) ||
                description != stored.description ||
                date != stored.occurredAt.date ||
                transactionType != stored.type ||
                accountSelected?.accountId != stored.accountId ||
                categorySelected?.categoryId != stored.categoryId
        }
}
