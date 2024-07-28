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
import kotlinx.coroutines.launch

class LoansViewModel(private val loanAndPaymentsCreator: LoanAndPaymentsCreator): ViewModel() {

    var amount by mutableStateOf("")
        private set

    var interest by mutableStateOf("")
        private set

    var startDate by mutableStateOf(DateUtils.currentDateAtReadableFormat())
        private set

    private var startDateInLong: Long = DateUtils.currentDateInMillis()

    var duration by mutableStateOf("")
        private set

    var frequencyType by mutableStateOf(FrequencyType.DAILY)
        private set

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

    fun updateFrequencyType(value: FrequencyType) {
        frequencyType = value
    }

    fun create() = viewModelScope.launch {
        val loanCreate = LoanCreate(
            amount = amount,
            interest = interest,
            startDate = startDateInLong,
            duration = duration,
            frequencyType = FrequencyType.DAILY
        )
        loanAndPaymentsCreator.create(loanCreate)
    }
}