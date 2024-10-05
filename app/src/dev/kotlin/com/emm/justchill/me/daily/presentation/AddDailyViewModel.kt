package com.emm.justchill.me.daily.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.hh.account.domain.DailyAccountCreator
import com.emm.justchill.hh.shared.DateAndTimeCombiner
import com.emm.justchill.hh.transaction.domain.TransactionCreator
import com.emm.justchill.hh.transaction.domain.TransactionInsert
import com.emm.justchill.hh.transaction.presentation.DateUtils
import com.emm.justchill.hh.transaction.presentation.TransactionType
import com.emm.justchill.me.daily.domain.Daily
import com.emm.justchill.me.daily.domain.DailyRepository
import com.emm.justchill.me.driver.domain.Driver
import com.emm.justchill.me.driver.domain.DriverRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.*

class AddDailyViewModel(
    driverRepository: DriverRepository,
    private val driverId: Long,
    private val dailyRepository: DailyRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
    private val transactionCreator: TransactionCreator,
    private val dailyAccountCreator: DailyAccountCreator,
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

    fun addDaily() = viewModelScope.launch {
        val daily = Daily(
            dailyId = UUID.randomUUID().toString(),
            amount = amount.toDouble(),
            dailyDate = dateAndTimeCombiner.combineWithUtc(dateInLong),
            driverId = driverId
        )
        dailyRepository.insert(daily)
        resolveTransactionAndAccounts()
    }

    private suspend fun resolveTransactionAndAccounts() {
        val accountId = dailyAccountCreator.create()

        val transactionInsert = TransactionInsert(
            type = TransactionType.INCOME,
            amount = amount.toDouble(),
            description = "Feria de ${currentDriver.value?.name}",
            date = dateAndTimeCombiner.combineWithUtc(dateInLong),
            accountId = accountId
        )
        transactionCreator.create(transactionInsert)
    }
}