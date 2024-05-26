package com.emm.retrofit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.emm.retrofit.domain.DrinkRepository

class VMFactory(private val drinkRepository: DrinkRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(drinkRepository) as T
    }

}