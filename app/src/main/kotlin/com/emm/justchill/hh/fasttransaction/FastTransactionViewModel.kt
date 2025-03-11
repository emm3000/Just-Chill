package com.emm.justchill.hh.fasttransaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.core.formatInputToDouble
import com.emm.domain.transaction.TransactionCreator
import com.emm.domain.transaction.TransactionInsert
import com.emm.justchill.hh.transaction.presentation.DateUtils
import com.emm.domain.transaction.TransactionType
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class FastTransactionViewModel(
    private val transactionCreator: TransactionCreator,
) : ViewModel() {

    var state by mutableStateOf(FastTransactionUiState())
        private set

    init {
        snapshotFlow { state.amount }
            .onEach {
                val isEnabled = state.amount.formatInputToDouble() > 1.0
                state = state.copy(isEnabled = isEnabled)
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: FastTransactionAction) {
        when (action) {
            is FastTransactionAction.AddTransaction -> addTransaction(action.accountId, action.type)
            is FastTransactionAction.OnAmountChange -> state = state.copy(amount = action.amount)
            is FastTransactionAction.OnDescriptionChange -> state = state.copy(description = action.description)
        }
    }

    private fun addTransaction(accountId: String, type: TransactionType) = viewModelScope.launch {
        val transactionInsert = TransactionInsert(
            type = type,
            amount = state.amount.formatInputToDouble(),
            description = state.description,
            date = DateUtils.currentDateInMillis(),
            accountId = accountId,
        )
        transactionCreator.create(transactionInsert)
    }
}