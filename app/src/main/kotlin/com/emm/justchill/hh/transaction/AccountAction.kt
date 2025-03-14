package com.emm.justchill.hh.transaction

import androidx.compose.ui.text.input.TextFieldValue
import com.emm.domain.account.Account
import com.emm.domain.transaction.TransactionType

sealed interface AccountAction {

    class OnAmountChange(val value: TextFieldValue) : AccountAction

    class OnDescriptionChange(val value: String) : AccountAction

    class OnDateChange(val value: String) : AccountAction

    class OnTransactionTypeChange(val value: TransactionType) : AccountAction

    class OnDateChangeInMillis(val value: Long?) : AccountAction

    data object OnSave : AccountAction

    data object OnDelete : AccountAction
}