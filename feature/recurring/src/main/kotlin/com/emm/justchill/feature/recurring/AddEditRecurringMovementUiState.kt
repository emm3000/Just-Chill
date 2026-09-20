package com.emm.justchill.feature.recurring

import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.category.SelectableCategory
import com.emm.justchill.core.ui.format.isSavableAmount
import com.emm.justchill.core.ui.mvi.UiState
import com.emm.justchill.core.ui.transaction.Catalog

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
    // The template's account until the user picks another. An id never expires, so it lands
    // whenever the catalog arrives — whichever of the two loads finishes first.
    val accountId: AccountId? = null,
    // Null is "Sin categoría", which the save writes as such.
    val categoryId: CategoryId? = null,
    // Bridges the frame between creating a category from this form and the repository flow
    // re-emitting with it.
    val extraCategories: List<SelectableCategory> = emptyList(),
    val openSheet: RecurringSheet? = null,
) : UiState {
    val accounts: List<Account> get() = catalog.accounts

    val categories: List<SelectableCategory> get() = categoriesOf(type.categoryType)

    // The first account is both the create-mode default and what a deleted account falls back to.
    val selectedAccount: Account? get() = accounts.find { it.accountId == accountId } ?: accounts.firstOrNull()

    val selectedCategory: SelectableCategory? get() = categories.find { it.categoryId == categoryId }

    val isSaveEnabled: Boolean
        get() = name.isNotBlank() && selectedAccount != null && (isVariableAmount || amountDigits.isSavableAmount())

    private fun categoriesOf(categoryType: CategoryType): List<SelectableCategory> {
        val known: List<SelectableCategory> = catalog.loaded?.categories?.get(categoryType).orEmpty()
        val pending: List<SelectableCategory> = extraCategories.filter { extra ->
            extra.categoryType == categoryType && known.none { it.categoryId == extra.categoryId }
        }
        return if (pending.isEmpty()) known else pending + known
    }
}
