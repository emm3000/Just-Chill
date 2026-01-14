package com.emm.justchill.hh.transaction

import androidx.compose.ui.text.input.TextFieldValue
import com.emm.domain.account.Account
import com.emm.domain.transaction.TransactionType

sealed interface AddTransactionAction {

    class OnAmountChange(val value: TextFieldValue) : AddTransactionAction

    class OnDescriptionChange(val value: String) : AddTransactionAction

    class OnDateChange(val value: String) : AddTransactionAction

    class OnTransactionTypeChange(val value: TransactionType) : AddTransactionAction

    class OnDateChangeInMillis(val value: Long?) : AddTransactionAction

    class OnAccountSelected(val value: Account) : AddTransactionAction

    data class OnCategorySelected(val value: SelectableCategory) : AddTransactionAction

    data object OnReset : AddTransactionAction

    data object OnSave : AddTransactionAction

    data object OnDelete : AddTransactionAction
}