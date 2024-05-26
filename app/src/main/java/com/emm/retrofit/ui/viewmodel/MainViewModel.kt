package com.emm.retrofit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.emm.retrofit.domain.DrinkRepository
import com.emm.retrofit.vo.Result
import kotlinx.coroutines.Dispatchers

class MainViewModel(private val drinkRepository: DrinkRepository) : ViewModel() {

    val fetchDrinks = liveData(Dispatchers.IO) {
        emit(Result.Loading)
        try {
            emit(drinkRepository.fetchByName("margarita"))
        } catch(ex: Exception) {
            emit(Result.Failure(ex))
        }
    }

}