package com.emm.justchill.hh.transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.account.Account
import com.emm.domain.account.AccountFinder
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionDeleter
import com.emm.domain.transaction.TransactionFinder
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionUpdate
import com.emm.domain.transaction.TransactionUpdater
import com.emm.justchill.core.formatInputToDouble
import com.emm.justchill.hh.transaction.DateUtils.millisToReadableFormat
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class EditTransactionViewModel(
    private val transactionId: String,
    private val transactionUpdater: TransactionUpdater,
    private val transactionFinder: TransactionFinder,
    private val transactionDeleter: TransactionDeleter,
    private val accountFinder: AccountFinder,
) : ViewModel() {

    var state by mutableStateOf(TransactionUiState())
        private set

    private var dateInLong: Long = DateUtils.currentDateInMillis()

    init {
        combine(
            snapshotFlow { state.amount },
            snapshotFlow { state.date },
            snapshotFlow { state.description },
        ) { mount, date, description ->
            val isEnabled = mount.formatInputToDouble() >= 1
                    && date.isNotEmpty()
                    && description.isNotEmpty()
            state = state.copy(isEnabled = isEnabled)
        }.launchIn(viewModelScope)

        loadCurrentTransaction()
    }

    fun onAction(action: AccountAction) {
        when (action) {
            is AccountAction.OnAmountChange -> state = state.copy(amount = action.value)
            is AccountAction.OnDateChange -> state = state.copy(date = action.value)
            is AccountAction.OnDescriptionChange -> state = state.copy(description = action.value)
            is AccountAction.OnTransactionTypeChange -> state = state.copy(transactionType = action.value)
            is AccountAction.OnDateChangeInMillis -> updateCurrentDate(action.value)
            AccountAction.OnSave -> updateTransaction()
            AccountAction.OnDelete -> deleteTransaction()
            is AccountAction.OnAccountSelected -> state = state.copy(accountSelected = action.value)
        }
    }

    private fun loadCurrentTransaction() = viewModelScope.launch {
        val currentTransaction: Transaction = transactionFinder.find(transactionId).firstOrNull() ?: return@launch
        val account: Account = accountFinder.find(currentTransaction.accountId) ?: return@launch
        state = state.copy(
            amount = TextFieldValue(currentTransaction.amountDecimalFormat),
            description = currentTransaction.description,
            date = millisToReadableFormat(currentTransaction.date),
            transactionType = TransactionType.valueOf(currentTransaction.type),
            accountSelected = account,
        )
        dateInLong = currentTransaction.date
    }

    private fun updateTransaction() = viewModelScope.launch {
        val transactionUpdate = TransactionUpdate(
            type = state.transactionType,
            description = state.description,
            date = dateInLong,
            amount = state.amount.formatInputToDouble(),
            accountId = state.accountSelected?.accountId ?: throw IllegalStateException()
        )
        transactionUpdater.update(transactionId, transactionUpdate)
    }

    private fun deleteTransaction() = viewModelScope.launch {
        transactionDeleter.delete(transactionId)
    }

    private fun updateCurrentDate(millis: Long?) = millis?.let {
        dateInLong = it
        state = state.copy(date = DateUtils.millisToReadableFormatUTC(it))
    }
}