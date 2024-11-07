package com.emm.justchill.hh.transaction.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.hh.account.domain.Account
import com.emm.justchill.hh.account.domain.AccountRepository
import com.emm.justchill.hh.transaction.domain.TransactionCreator
import com.emm.justchill.hh.transaction.domain.TransactionInsert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val transactionCreator: TransactionCreator,
    accountRepository: AccountRepository,
) : ViewModel() {

    var amount by mutableStateOf("")
        private set

    var description by mutableStateOf("")
        private set

    var date by mutableStateOf(DateUtils.currentDateAtReadableFormat())
        private set

    private var dateInLong: Long = DateUtils.currentDateInMillis()

    var transactionType: TransactionType by mutableStateOf(TransactionType.INCOME)
        private set


    private val _state = MutableStateFlow(TransactionState())
    val state: StateFlow<TransactionState> = combine(
        _state,
        accountRepository.retrieve(),
    ) { transactionState, accounts ->
        _state.update {
            it.copy(
                accounts = accounts,
                accountSelected = transactionState.accountSelected ?: accounts.firstOrNull()
            )
        }
        _state.value
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = TransactionState()
        )

    init {
        combine(
            snapshotFlow { amount },
            snapshotFlow { date },
            snapshotFlow { description },
        ) { mount, date, description ->
            _state.update {
                it.copy(
                    isEnabledButton = mount.isNotEmpty()
                            && date.isNotEmpty()
                            && description.isNotEmpty()
                            && _state.value.accountSelected != null
                )
            }
        }.launchIn(viewModelScope)
    }

    fun addTransaction() = viewModelScope.launch {
        val accountId: String = _state.value.accountSelected?.accountId ?: throw IllegalStateException()
        val transactionInsert = TransactionInsert(
            type = transactionType,
            description = description,
            date = dateInLong,
            amount = amount.toDouble(),
            accountId = accountId
        )
        transactionCreator.create(transactionInsert)
    }

    fun updateAmount(value: String) {
        amount = value
    }

    fun updateDescription(value: String) {
        description = value
    }

    fun updateCurrentDate(millis: Long?) {
        if (millis != null) {
            dateInLong = millis
            date = DateUtils.millisToReadableFormatUTC(millis)
        }
    }

    fun updateTransactionType(value: TransactionType) {
        transactionType = value
    }

    fun updateAccountSelected(value: Account) {
        _state.update {
            it.copy(
                accountSelected = value
            )
        }
    }
}