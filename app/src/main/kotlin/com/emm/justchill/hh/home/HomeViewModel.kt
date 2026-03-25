package com.emm.justchill.hh.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.home.HomeData
import com.emm.domain.home.GetHomeDataUseCase
import com.emm.justchill.hh.transaction.toUi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(homeLoader: GetHomeDataUseCase) : ViewModel() {

    val state: StateFlow<HomeUiState> = homeLoader()
        .map(::mapToUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState(),
        )

    private fun mapToUiState(data: HomeData): HomeUiState = HomeUiState(
        lastTransactions = data.lastTransactions.toUi(),
        income = data.income,
        spend = data.spend,
        balance = data.balance
    )
}
