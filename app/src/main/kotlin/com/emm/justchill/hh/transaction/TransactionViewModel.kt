package com.emm.justchill.hh.transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.core.formatInputToDouble
import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.transaction.TransactionCreator
import com.emm.domain.transaction.TransactionInsert
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val transactionCreator: TransactionCreator,
    accountRepository: AccountRepository,
) : ViewModel() {

    private var dateInLong: Long = DateUtils.currentDateInMillis()

    var state by mutableStateOf(TransactionUiState())
        private set

    val accounts: StateFlow<List<Account>> = accountRepository.retrieve()
        .onEach(::pickFirstAccount)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    init {
        combine(
            flow = snapshotFlow { state.amount },
            flow2 = snapshotFlow { state.date },
            flow3 = snapshotFlow { state.description },
            flow4 = snapshotFlow { state.accountSelected },
            transform = ::validateFields,
        ).launchIn(viewModelScope)
    }

    private fun validateFields(
        mount: TextFieldValue,
        date: String,
        description: String,
        account: Account?
    ) {
        val isEnabled = mount.formatInputToDouble() >= 1.0
                && date.isNotEmpty()
                && description.isNotEmpty()
                && account != null
        state = state.copy(isEnabled = isEnabled)
    }

    fun onAction(action: AccountAction) {
        when (action) {
            is AccountAction.OnAmountChange -> state = state.copy(amount = action.value)
            is AccountAction.OnDateChange -> state = state.copy(date = action.value)
            is AccountAction.OnDescriptionChange -> state = state.copy(description = action.value)
            is AccountAction.OnTransactionTypeChange -> state = state.copy(transactionType = action.value)
            is AccountAction.OnAccountSelected -> state = state.copy(accountSelected = action.account)
            is AccountAction.OnDateChangeInMillis -> updateCurrentDate(action.value)
            AccountAction.OnSave -> addTransaction()
            else -> {}
        }
    }

    private fun addTransaction() = viewModelScope.launch {
        val transactionInsert = TransactionInsert(
            type = state.transactionType,
            description = state.description,
            date = dateInLong,
            amount = state.amount.formatInputToDouble(),
            accountId = state.accountSelected?.accountId ?: throw IllegalStateException()
        )
        transactionCreator.create(transactionInsert)
    }

    private fun pickFirstAccount(accounts: List<Account>) {
        state = state.copy(accountSelected = state.accountSelected ?: accounts.firstOrNull())
    }

    private fun updateCurrentDate(millis: Long?) = millis?.let {
        dateInLong = it
        state = state.copy(date = DateUtils.millisToReadableFormatUTC(it))
    }
}