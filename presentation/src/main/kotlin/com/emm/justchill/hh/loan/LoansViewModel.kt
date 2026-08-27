package com.emm.justchill.hh.loan

import androidx.lifecycle.viewModelScope
import com.emm.domain.loan.LoanRepository
import com.emm.justchill.core.mvi.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LoansViewModel(loanRepository: LoanRepository) :
    MviViewModel<LoansUiState, LoansIntent, LoansEffect>(LoansUiState()) {

    init {
        loanRepository.balancesByPerson()
            .onEach { balances -> updateState { copy(people = balances.toUi()) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: LoansIntent) {
        when (intent) {
            is LoansIntent.OnPersonClick -> sendEffect(LoansEffect.NavigateToPerson(intent.personKey))
            LoansIntent.OnAddLoanClick -> sendEffect(LoansEffect.NavigateToAddLoan)
        }
    }
}
