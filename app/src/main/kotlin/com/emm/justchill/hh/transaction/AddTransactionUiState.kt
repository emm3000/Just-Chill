package com.emm.justchill.hh.transaction

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import com.emm.domain.account.Account
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.shared.Empty

@Stable
data class AddTransactionUiState(
    val amount: TextFieldValue = TextFieldValue("0.00"),
    val description: String = String.Empty,
    val date: String = DateUtils.currentDateAtReadableFormat(),
    val transactionType: TransactionType = TransactionType.Income,
    val isEnabled: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val accountSelected: Account? = null,
    val categories: List<SelectableCategory> = emptyList(),
    val categorySelected: SelectableCategory? = null,
) : UiState
