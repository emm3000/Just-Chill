package com.emm.justchill.quota

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.quota.domain.Driver
import com.emm.justchill.quota.domain.DriverRepository
import com.emm.justchill.quota.domain.Quota
import com.emm.justchill.quota.domain.QuotaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class QuotasViewModel(
    driverId: Long,
    driverRepository: DriverRepository,
    quotaRepository: QuotaRepository,
) : ViewModel() {

    val driver: StateFlow<Driver?> = driverRepository.find(driverId)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    val quotas: StateFlow<List<QuotaUi>> = quotaRepository.retrieveBy(driverId)
        .map { it.map(Quota::toUi) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
}