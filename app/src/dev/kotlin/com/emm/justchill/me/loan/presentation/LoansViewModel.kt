package com.emm.justchill.me.loan.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.me.loan.domain.Loan
import com.emm.justchill.me.loan.domain.LoanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoansViewModel(
    driverId: Long,
    private val loanRepository: LoanRepository
): ViewModel() {

    val loans: StateFlow<List<LoanUi>> = loanRepository.retrieveByDriverId(driverId)
        .map {
            it.map(Loan::toUi)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun delete(loanId: String) = viewModelScope.launch {
        loanRepository.delete(loanId)
    }
}