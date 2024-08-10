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
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoansViewModel(private val loanAndPaymentsCreator: LoanAndPaymentsCreator) : ViewModel() {

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

    private val _uiState = MutableStateFlow(LoansUiState())
    val uiState: StateFlow<LoansUiState> get() = _uiState.asStateFlow()

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
        tryCreateLoanAndPaymentsCreator()
    }

    private suspend fun tryCreateLoanAndPaymentsCreator() = try {
        showLoading()
        val loanCreate = createLoanCreate()
        loanAndPaymentsCreator.create(loanCreate)
        onSuccess()
    } catch (e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
        onFailure(e)
    }

    private fun onFailure(e: Throwable) {
        _uiState.update { state -> state.copy(isLoading = false, isError = e.message) }
    }

    private fun onSuccess() {
        _uiState.update { state -> state.copy(isLoading = false, isSuccess = true) }
    }

    private fun showLoading() {
        _uiState.update { state -> state.copy(isLoading = true) }
    }

    fun dismissErrorDialog() {
        _uiState.update { state -> state.copy(isLoading = false, isError = null) }
    }

    private fun createLoanCreate() = LoanCreate(
        amount = amount,
        interest = interest,
        startDate = startDateInLong,
        duration = duration,
        frequencyType = FrequencyType.DAILY
    )
}

data class LoansUiState(
    val isLoading: Boolean = false,
    val isError: String? = null,
    val isSuccess: Boolean = false,
)