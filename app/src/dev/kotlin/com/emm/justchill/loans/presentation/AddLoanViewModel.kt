package com.emm.justchill.loans.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.hh.presentation.transaction.DateUtils
import com.emm.justchill.loans.domain.FrequencyType
import com.emm.justchill.loans.domain.LoanAndPaymentsCreator
import com.emm.justchill.loans.domain.LoanCreate
import com.emm.justchill.quota.domain.Driver
import com.emm.justchill.quota.domain.DriverRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddLoanViewModel(
    driverRepository: DriverRepository,
    private val driverId: Long,
    private val loanAndPaymentsCreator: LoanAndPaymentsCreator,
) : ViewModel() {

    var amount by mutableStateOf("")
        private set

    var interest by mutableStateOf("")
        private set

    var startDate by mutableStateOf(DateUtils.currentDateAtReadableFormat())
        private set

    private var startDateInLong: Long = DateUtils.currentDateInMillis()

    var duration by mutableStateOf("")
        private set

    val currentDriver: StateFlow<Driver?> = driverRepository.find(driverId)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    fun updateAmount(value: String) {
        amount = value
    }

    fun updateInterest(value: String) {
        interest = value
    }

    fun updateStartDate(millis: Long?) {
        if (millis != null) {
            startDateInLong = millis
            startDate = DateUtils.millisToReadableFormatUTC(millis)
        }
    }

    fun updateDuration(value: String) {
        duration = value
    }

    fun create() = viewModelScope.launch {
        tryCreateLoanAndPaymentsCreator()
    }

    private suspend fun tryCreateLoanAndPaymentsCreator() = try {
        val loanCreate = createLoanCreate()
        loanAndPaymentsCreator.create(loanCreate)
    } catch (e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
    }

    private fun createLoanCreate() = LoanCreate(
        amount = amount,
        interest = interest,
        startDate = startDateInLong,
        duration = duration,
        frequencyType = FrequencyType.DAILY,
        driverId = driverId,
    )
}