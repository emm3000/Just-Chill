package com.emm.retrofit.experiences.drinks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.retrofit.experiences.drinks.data.DrinkApiModel
import com.emm.retrofit.core.Result
import com.emm.retrofit.experiences.drinks.domain.DrinkFetcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(drinkFetcher: DrinkFetcher) : ViewModel() {

    val fetchDrinks: StateFlow<Result<List<DrinkApiModel>>> = drinkFetcher.fetch("margarita")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = Result.Loading
        )
}