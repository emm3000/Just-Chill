package com.emm.justchill.hh.transaction

import com.emm.domain.account.Account
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.shared.Empty

data class EditTransactionUiState(
    val amount: String = "",
    val description: String = String.Empty,
    val date: String = DateUtils.currentDateAtReadableFormat(),
    val transactionType: TransactionType = TransactionType.Income,
    val isEnabled: Boolean = false,
    val hasChanges: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val accountSelected: Account? = null,
    val categories: List<SelectableCategory> = emptyList(),
    val categorySelected: SelectableCategory? = null,
    val frequentCategoryIds: List<String> = emptyList(),
) : UiState {
    val missingField: MissingField? get() = when {
        centsToSoles(amount) <= 0.0 -> MissingField.Amount
        accountSelected == null -> MissingField.Account
        else -> null
    }
}
