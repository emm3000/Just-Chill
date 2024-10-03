package com.emm.justchill.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.daily.domain.Driver
import com.emm.justchill.daily.domain.DriverRepository
import com.emm.justchill.daily.domain.Daily
import com.emm.justchill.daily.domain.DailyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DailiesViewModel(
    driverId: Long,
    driverRepository: DriverRepository,
    private val dailyRepository: DailyRepository,
) : ViewModel() {

    val driver: StateFlow<Driver?> = driverRepository.find(driverId)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    val dailies: StateFlow<List<DailyUi>> = dailyRepository.retrieveBy(driverId)
        .map { it.map(Daily::toUi) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun deleteDaily(dailyId: String) = viewModelScope.launch {
        dailyRepository.deleteBy(dailyId)
    }
}