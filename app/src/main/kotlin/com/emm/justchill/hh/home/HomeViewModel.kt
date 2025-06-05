@file:OptIn(ExperimentalCoroutinesApi::class)

package com.emm.justchill.hh.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.transaction.TransactionSumIncome
import com.emm.domain.transaction.TransactionSumSpend
import com.emm.justchill.hh.shared.fromCentsToSolesWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val transactionSumIncome: TransactionSumIncome,
    private val transactionSumSpend: TransactionSumSpend,
    accountRepository: AccountRepository,
) : ViewModel() {

    val calculators: StateFlow<HomeUiState> = accountRepository.default()
        .filterNotNull()
        .flatMapLatest(::aggregateAccount)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = HomeUiState()
        )

    private fun aggregateAccount(account: Account): Flow<HomeUiState> = combine(
        flow = transactionSumIncome(account.accountId),
        flow2 = transactionSumSpend(account.accountId),
        transform = { income, spend ->
            HomeUiState(
                income = fromCentsToSolesWith(income),
                spend = fromCentsToSolesWith(spend),
                account = account,
            )
        }
    )
}