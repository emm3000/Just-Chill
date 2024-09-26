package com.emm.justchill.quota

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.quota.domain.Driver
import com.emm.justchill.quota.domain.DriverRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val staticDrivers = listOf(
    Driver(
        driverId = 1,
        name = "Sergio",
    ),
    Driver(
        driverId = 2,
        name = "Soldado",
    ),
    Driver(
        driverId = 3,
        name = "Nehemias",
    ),
    Driver(
        driverId = 4,
        name = "Samora",
    )
)

class DriversViewModel(
    private val driverRepository: DriverRepository,
) : ViewModel() {

    val drivers: StateFlow<List<Driver>> = driverRepository
        .all()
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