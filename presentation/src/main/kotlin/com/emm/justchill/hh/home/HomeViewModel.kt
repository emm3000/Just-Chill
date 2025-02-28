@file:OptIn(ExperimentalCoroutinesApi::class)

package com.emm.justchill.hh.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.justchill.hh.shared.fromCentsToSolesWith
import com.emm.domain.transaction.TransactionDifferenceCalculator
import com.emm.domain.transaction.TransactionSumIncome
import com.emm.domain.transaction.TransactionSumSpend
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val transactionSumIncome: TransactionSumIncome,
    private val transactionSumSpend: TransactionSumSpend,
    private val transactionDifferenceCalculator: TransactionDifferenceCalculator,
    accountRepository: AccountRepository,
) : ViewModel() {

    var accountSelected: Account? by mutableStateOf(null)
        private set

    val accounts: StateFlow<List<Account>> = accountRepository.retrieve()
        .onEach { accounts ->
            accountSelected = accounts.firstOrNull()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val calculators: StateFlow<HomeState> = snapshotFlow { accountSelected }
        .filterNotNull()
        .flatMapLatest(::extract)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = HomeState()
        )

    fun updateAccountSelected(value: Account) {
        accountSelected = value
    }

    private fun extract(
        account: Account
    ): Flow<HomeState> = combine(
        transactionSumIncome(account.accountId),
        transactionSumSpend(account.accountId),
        transactionDifferenceCalculator.calculate(account.accountId)
    ) { income, spend, difference ->
        HomeState(
            difference = fromCentsToSolesWith(difference),
            income = fromCentsToSolesWith(income),
            spend = fromCentsToSolesWith(spend)
        )
    }
}