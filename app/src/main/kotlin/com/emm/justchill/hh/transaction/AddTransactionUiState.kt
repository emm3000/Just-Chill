package com.emm.justchill.hh.transaction

import androidx.compose.runtime.Stable
import com.emm.domain.account.Account
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.shared.Empty

@Stable
data class AddTransactionUiState(
    val amount: String = "",
    val description: String = String.Empty,
    val date: String = DateUtils.currentDateAtReadableFormat(),
    val transactionType: TransactionType = TransactionType.Income,
    val isEnabled: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val accountSelected: Account? = null,
    val categories: List<SelectableCategory> = emptyList(),
    val categorySelected: SelectableCategory? = null,
) : UiState
