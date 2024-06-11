package com.emm.retrofit.experiences.drinks.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.retrofit.core.Result
import com.emm.retrofit.experiences.drinks.data.DrinkApiModel
import com.emm.retrofit.experiences.drinks.domain.DrinkFetcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.stateIn

class MainViewModel(drinkFetcher: DrinkFetcher) : ViewModel() {

    var searchText: String by mutableStateOf("")
        private set

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val fetchDrinks: StateFlow<Result<List<DrinkApiModel>>> = snapshotFlow { searchText }
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapConcat(drinkFetcher::fetch)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = Result.Loading
        )

    fun updateSearchText(value: String) {
        searchText = value
    }
}