package com.emm.justchill.loans.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.daily.domain.Driver
import com.emm.justchill.daily.domain.DriverRepository
import com.emm.justchill.daily.staticDrivers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val driverRepository: DriverRepository,
): ViewModel() {

    val drivers: StateFlow<List<Driver>> = driverRepository.all()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    init {
        insertIfNoExistData()
    }

    private fun insertIfNoExistData() = viewModelScope.launch {
        val count: List<Driver> = driverRepository.all().firstOrNull().orEmpty()
        if (count.isNotEmpty()) return@launch
        staticDrivers.forEach {
            driverRepository.insert(it)
        }
    }
}