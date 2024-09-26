package com.emm.justchill.quota

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.hh.domain.shared.DateAndTimeCombiner
import com.emm.justchill.hh.presentation.transaction.DateUtils
import com.emm.justchill.quota.domain.Driver
import com.emm.justchill.quota.domain.DriverRepository
import com.emm.justchill.quota.domain.Quota
import com.emm.justchill.quota.domain.QuotaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class AddQuotaViewModel(
    driverRepository: DriverRepository,
    private val driverId: Long,
    private val quotaRepository: QuotaRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
) : ViewModel() {

    var amount: String by mutableStateOf("")
        private set

    var date: String by mutableStateOf(DateUtils.currentDateAtReadableFormat())
        private set

    private var dateInLong: Long = DateUtils.currentDateInMillis()

    var isEnabled: Boolean by mutableStateOf(false)
        private set

    val currentDriver: StateFlow<Driver?> = driverRepository.find(driverId)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    init {
        snapshotFlow { amount }
            .onEach {
                isEnabled = amount.isNotEmpty()
            }.launchIn(viewModelScope)
    }

    fun updateAmount(value: String) {
        amount = value
    }

    fun updateCurrentDate(millis: Long?) {
        if (millis != null) {
            dateInLong = millis
            date = DateUtils.millisToReadableFormatUTC(millis)
        }
    }

    fun addQuota() = viewModelScope.launch {
        val quota = Quota(
            quoteId = UUID.randomUUID().toString(),
            amount = amount.toDouble(),
            quoteDate = dateAndTimeCombiner.combine(dateInLong),
            driverId = driverId
        )
        quotaRepository.insert(quota)
    }
}