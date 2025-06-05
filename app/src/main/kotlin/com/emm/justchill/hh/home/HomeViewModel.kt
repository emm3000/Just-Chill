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
        .map(HomeUiState::Success)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState.Loading,
        )
}

sealed interface HomeUiState {

    data object Loading: HomeUiState

    data class Success(val data: HomeData): HomeUiState
}