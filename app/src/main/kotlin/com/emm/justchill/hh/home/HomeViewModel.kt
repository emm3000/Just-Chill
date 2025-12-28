package com.emm.justchill.hh.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.home.HomeData
import com.emm.domain.home.HomeLoader
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(homeLoader: HomeLoader) : ViewModel() {

    val state: StateFlow<HomeUiState> = homeLoader
        .load()
        .map(::mapToUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState(),
        )

    private fun mapToUiState(data: HomeData): HomeUiState = HomeUiState(
        lastTransactions = data.lastTransactions,
        income = data.income,
        spend = data.spend,
        balance = data.balance
    )
}