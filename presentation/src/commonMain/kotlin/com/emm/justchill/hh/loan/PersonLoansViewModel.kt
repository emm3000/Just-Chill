package com.emm.justchill.hh.loan

import androidx.lifecycle.viewModelScope
import com.emm.domain.loan.DeleteLoanUseCase
import com.emm.domain.loan.LoanRepository
import com.emm.domain.shared.LoanId
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class PersonLoansViewModel(
    private val personKey: String,
    loanRepository: LoanRepository,
    private val deleteLoan: DeleteLoanUseCase,
) : MviViewModel<PersonLoansUiState, PersonLoansIntent, PersonLoansEffect>() {

    override val initialState = PersonLoansUiState()

    init {
        loanRepository.loansWithBalance(personKey)
            .onEach { balances ->
                updateState {
                    // loansWithBalance orders by lentAt DESC, so the first row is this person's
                    // most recent loan — the same "name follows the latest loan" rule balancesByPerson uses.
                    copy(personName = balances.firstOrNull()?.loan?.personName.orEmpty(), loans = balances.toUi())
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: PersonLoansIntent) {
        when (intent) {
            is PersonLoansIntent.OnAddPaymentClick -> {
                sendEffect(PersonLoansEffect.NavigateToAddPayment(intent.loanId))
            }

            is PersonLoansIntent.OnEditLoanClick -> sendEffect(PersonLoansEffect.NavigateToEditLoan(intent.loanId))

            is PersonLoansIntent.OnDeleteClick -> updateState { copy(pendingDelete = intent.loanId) }

            PersonLoansIntent.OnDeleteDismiss -> updateState { copy(pendingDelete = null) }

            PersonLoansIntent.OnDeleteConfirm -> confirmDelete()
        }
    }

    private fun confirmDelete() = launchSafe(
        onError = { e ->
            updateState { copy(pendingDelete = null) }
            PersonLoansEffect.ShowError(e.toUserMessage())
        },
    ) {
        val target = currentState.pendingDelete ?: return@launchSafe
        deleteLoan(LoanId(target))
        updateState { copy(pendingDelete = null) }
    }
}
