package com.emm.justchill.hh.loan

import androidx.lifecycle.viewModelScope
import com.emm.domain.loan.LoanRepository
import com.emm.justchill.core.mvi.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class PersonLoansViewModel(personKey: String, loanRepository: LoanRepository) :
    MviViewModel<PersonLoansUiState, PersonLoansIntent, PersonLoansEffect>(PersonLoansUiState()) {

    init {
        loanRepository.loansWithBalance(personKey)
            .onEach { balances ->
                updateState {
                    copy(
                        personName = balances.firstOrNull()?.loan?.personName ?: currentState.personName,
                        loans = balances.toUi(),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: PersonLoansIntent) {
        when (intent) {
            is PersonLoansIntent.OnLoanClick -> sendEffect(PersonLoansEffect.NavigateToLoanDetail(intent.loanId))
        }
    }
}
