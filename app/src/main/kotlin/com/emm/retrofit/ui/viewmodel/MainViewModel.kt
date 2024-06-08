package com.emm.retrofit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.retrofit.data.model.Drink
import com.emm.retrofit.domain.DrinkRepository
import com.emm.retrofit.core.Result
import com.emm.retrofit.core.asResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(drinkRepository: DrinkRepository) : ViewModel() {

    val fetchDrinks: StateFlow<Result<List<Drink>>> = drinkRepository.fetchByName("margarita")
        .asResult()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = Result.Loading
        )
}