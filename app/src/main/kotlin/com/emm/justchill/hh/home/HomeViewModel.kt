@file:OptIn(ExperimentalCoroutinesApi::class)

package com.emm.justchill.hh.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.hh.account.domain.Account
import com.emm.justchill.hh.account.domain.AccountRepository
import com.emm.justchill.hh.shared.BackupManager
import com.emm.justchill.hh.transaction.domain.TransactionDifferenceCalculator
import com.emm.justchill.hh.transaction.domain.TransactionSumIncome
import com.emm.justchill.hh.transaction.domain.TransactionSumSpend
import com.emm.justchill.hh.shared.fromCentsToSolesWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    transactionSumIncome: TransactionSumIncome,
    transactionSumSpend: TransactionSumSpend,
    transactionDifferenceCalculator: TransactionDifferenceCalculator,
    accountRepository: AccountRepository,
    private val backupManager: BackupManager,
) : ViewModel() {

    var account: Account? by mutableStateOf(null)
        private set

    val sumTransactions: StateFlow<Pair<String, String>> = snapshotFlow { account }
        .filterNotNull()
        .flatMapLatest {
            combine(
                transactionSumIncome(it.accountId),
                transactionSumSpend(it.accountId),
            ) { income, spend ->
                Pair(fromCentsToSolesWith(income), fromCentsToSolesWith(spend))
            }
        }
        .catch {
            emit(Pair("0.00", "0.00"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = Pair("0.00", "0.00")
        )

    val difference: StateFlow<String> = snapshotFlow { account }
        .filterNotNull()
        .map { it.accountId }
        .flatMapLatest(transactionDifferenceCalculator::calculate)
        .map(::fromCentsToSolesWith)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = "0.00"
        )

    val accounts: StateFlow<List<Account>> = accountRepository.retrieve()
        .onEach { accounts ->
            accounts.firstOrNull()?.let {
                accountLabel = it.nameWithBalance
                account = it
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    var accountLabel: String by mutableStateOf("")
        private set

//    init {
//        viewModelScope.launch {
//            backupManager.seed()
//        }
//    }

    fun updateAccountLabel(value: String) {
        accountLabel = value
    }

    fun updateAccountSelected(value: Account) {
        account = value
    }
}