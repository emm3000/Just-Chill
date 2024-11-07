@file:OptIn(ExperimentalCoroutinesApi::class)

package com.emm.justchill.hh.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.hh.account.domain.Account
import com.emm.justchill.hh.account.domain.AccountRepository
import com.emm.justchill.hh.shared.fromCentsToSolesWith
import com.emm.justchill.hh.transaction.domain.TransactionDifferenceCalculator
import com.emm.justchill.hh.transaction.domain.TransactionSumIncome
import com.emm.justchill.hh.transaction.domain.TransactionSumSpend
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class HomeViewModel(
    private val transactionSumIncome: TransactionSumIncome,
    private val transactionSumSpend: TransactionSumSpend,
    private val transactionDifferenceCalculator: TransactionDifferenceCalculator,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state
        .flatMapLatest { homeState ->
            val accountId = homeState.accountSelected?.accountId.orEmpty()
            combine(
                accountRepository.retrieve(),
                transactionSumIncome(accountId),
                transactionSumSpend(accountId),
                transactionDifferenceCalculator.calculate(accountId),
            ) { accounts, income, spend, difference ->
                _state.update {
                    it.copy(
                        accounts = accounts,
                        accountSelected = homeState.accountSelected ?: accounts.firstOrNull(),
                        income = fromCentsToSolesWith(income),
                        spend = fromCentsToSolesWith(spend),
                        difference = fromCentsToSolesWith(difference),
                    )
                }
                _state.value
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = HomeState()
        )

    fun updateAccountSelected(value: Account) {
        _state.update { it.copy(accountSelected = value) }
    }
}