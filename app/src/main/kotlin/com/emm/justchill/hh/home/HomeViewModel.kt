package com.emm.justchill.hh.home

import androidx.lifecycle.viewModelScope
import com.emm.domain.home.GetHomeDataUseCase
import com.emm.domain.home.HomeData
import com.emm.domain.shared.YearMonth
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.hh.transaction.toUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val getHomeData: GetHomeDataUseCase,
) : MviViewModel<HomeUiState, HomeIntent, HomeEffect>() {

    override val initialState = HomeUiState()

    private val selectedMonth = MutableStateFlow(YearMonth.current())

    init {
        selectedMonth
            .flatMapLatest { month -> getHomeData(month) }
            .onEach { homeData -> updateState { mapToUiState(homeData) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.PreviousMonth -> selectedMonth.value = selectedMonth.value.previous()
            HomeIntent.NextMonth -> selectedMonth.value = selectedMonth.value.next()
            HomeIntent.JumpToToday -> selectedMonth.value = YearMonth.current()
        }
    }

    private fun HomeUiState.mapToUiState(data: HomeData): HomeUiState = copy(
        month = selectedMonth.value,
        lastTransactions = data.lastTransactions.toUi(),
        income = data.income,
        spend = data.spend,
        balance = data.balance,
    )
}
