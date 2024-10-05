@file:OptIn(ExperimentalCoroutinesApi::class)

package com.emm.justchill.hh.shared.seetransactions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.hh.account.domain.Account
import com.emm.justchill.hh.account.domain.AccountRepository
import com.emm.justchill.hh.transaction.domain.TransactionLoader
import com.emm.justchill.hh.transaction.domain.Transaction
import com.emm.justchill.hh.transaction.presentation.TransactionUi
import com.emm.justchill.hh.transaction.presentation.toUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class SeeTransactionsViewModel(
    transactionLoader: TransactionLoader,
    accountRepository: AccountRepository,
) : ViewModel() {

    private var accountId: String by mutableStateOf("")

    var accountLabel: String by mutableStateOf("")
        private set

    val accounts: StateFlow<List<Account>> = accountRepository.retrieve()
        .onEach { accounts ->
            accounts.firstOrNull()?.let {
                accountLabel = "${it.name} ${it.balance}"
                accountId = it.accountId
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val transactions: StateFlow<List<TransactionUi>> = snapshotFlow { accountId }
        .flatMapLatest { transactionLoader.load(accountId) }
        .map(List<Transaction>::toUi)
        .catch(::catchThrowable)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    @Suppress("UNUSED_PARAMETER")
    private suspend fun catchThrowable(
        collector: FlowCollector<List<TransactionUi>>,
        throwable: Throwable,
    ) = collector.emit(emptyList())

    fun updateAccountLabel(value: String) {
        accountLabel = value
    }

    fun updateAccountSelected(value: Account) {
        accountId = value.accountId
    }
}