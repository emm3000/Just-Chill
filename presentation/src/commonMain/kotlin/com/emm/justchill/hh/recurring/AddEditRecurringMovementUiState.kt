package com.emm.justchill.hh.recurring

import com.emm.domain.account.Account
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.transaction.Catalog
import com.emm.justchill.hh.transaction.SelectableCategory
import com.emm.justchill.hh.transaction.isSavableAmount

data class AddEditRecurringMovementUiState(
    val isEdit: Boolean = false,
    val name: String = "",
    val type: TransactionType = TransactionType.Spend,
    val amountDigits: String = "",
    val isVariableAmount: Boolean = false,
    val dayOfMonth: Int = 1,
    val isActive: Boolean = true,
    val description: String = "",
    val catalog: Catalog = Catalog.Loading,
    /**
     * The template's account until the user picks another. An id never expires, so it lands
     * whenever the catalog arrives — whichever of the two loads finishes first.
     */
    val accountId: AccountId? = null,
    /** Null is "Sin categoría", which the save writes as such. */
    val categoryId: CategoryId? = null,
) : UiState {
    val accounts: List<Account> get() = loadedCatalog?.accounts.orEmpty()

    val categories: List<SelectableCategory> get() = loadedCatalog?.categories?.get(type.categoryType).orEmpty()

    /** The first account is both the create-mode default and what a deleted account falls back to. */
    val selectedAccount: Account? get() = accounts.find { it.accountId == accountId } ?: accounts.firstOrNull()

    val selectedCategory: SelectableCategory? get() = categories.find { it.categoryId == categoryId }

    val isSaveEnabled: Boolean
        get() = name.isNotBlank() && selectedAccount != null && (isVariableAmount || amountDigits.isSavableAmount())

    private val loadedCatalog: Catalog.Loaded? get() = catalog as? Catalog.Loaded
}
