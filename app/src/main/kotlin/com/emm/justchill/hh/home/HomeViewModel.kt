package com.emm.justchill.hh.home

import androidx.lifecycle.viewModelScope
import com.emm.domain.home.GetHomeDataUseCase
import com.emm.domain.home.HomeData
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.hh.transaction.toUi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class HomeViewModel(
    homeLoader: GetHomeDataUseCase,
) : MviViewModel<HomeUiState, HomeIntent, HomeEffect>(HomeUiState()) {

    init {
        homeLoader()
            .map(::mapToUiState)
            .onEach { newState -> updateState { newState } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: HomeIntent) = Unit

    private fun mapToUiState(data: HomeData): HomeUiState = HomeUiState(
        lastTransactions = data.lastTransactions.toUi(),
        income = data.income,
        spend = data.spend,
        balance = data.balance,
    )
}
