package com.emm.justchill.hh.loan

import androidx.lifecycle.viewModelScope
import com.emm.domain.loan.DeleteLoanUseCase
import com.emm.domain.loan.LoanPaymentInsert
import com.emm.domain.loan.LoanRepository
import com.emm.domain.loan.RegisterLoanPaymentUseCase
import com.emm.domain.shared.LoanId
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.hh.transaction.centsToMoney
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class PersonLoansViewModel(
    private val personKey: String,
    loanRepository: LoanRepository,
    private val deleteLoan: DeleteLoanUseCase,
    private val registerLoanPayment: RegisterLoanPaymentUseCase,
    private val clock: Clock,
    private val zone: TimeZone,
) : MviViewModel<PersonLoansUiState, PersonLoansIntent, PersonLoansEffect>() {

    override val initialState = PersonLoansUiState()

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
            is PersonLoansIntent.OnAddPaymentClick -> {
                updateState { copy(payment = LoanPaymentFormUi(loanId = intent.loanId, today = today())) }
            }

            is PersonLoansIntent.OnEditLoanClick -> sendEffect(PersonLoansEffect.NavigateToEditLoan(intent.loanId))

            is PersonLoansIntent.OnDeleteClick -> updateState { copy(pendingDelete = intent.loanId) }

            PersonLoansIntent.OnDeleteDismiss -> updateState { copy(pendingDelete = null) }

            PersonLoansIntent.OnDeleteConfirm -> confirmDelete()

            is PersonLoansIntent.OnPaymentAmountChange -> updatePayment { copy(amountDigits = intent.digits) }

            is PersonLoansIntent.OnPaymentMethodChange -> updatePayment { copy(method = intent.method) }

            is PersonLoansIntent.OnPaymentDateSelected -> updatePayment { copy(date = intent.value) }

            is PersonLoansIntent.OnPaymentNoteChange -> updatePayment { copy(note = intent.value) }

            PersonLoansIntent.OnPaymentDismiss -> updateState { copy(payment = null) }

            PersonLoansIntent.OnPaymentConfirm -> confirmPayment()
        }
    }

    private fun updatePayment(reducer: LoanPaymentFormUi.() -> LoanPaymentFormUi) {
        updateState { copy(payment = payment?.reducer()) }
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

    /**
     * Left open on failure, unlike confirmDelete: the form still holds a typed amount worth
     * fixing, where the delete dialog has nothing left to edit.
     */
    private fun confirmPayment() = launchSafe(
        onError = { e ->
            updateState { copy(payment = payment?.copy(isSaving = false)) }
            PersonLoansEffect.ShowError(e.toUserMessage())
        },
    ) {
        val form = currentState.payment ?: return@launchSafe
        if (form.isSaving) return@launchSafe
        updateState { copy(payment = payment?.copy(isSaving = true)) }
        val timeOfDay = clock.now().toLocalDateTime(zone).time
        registerLoanPayment(
            LoanPaymentInsert(
                loanId = LoanId(form.loanId),
                amount = centsToMoney(form.amountDigits),
                method = form.method,
                paidAt = LocalDateTime(form.date ?: form.today, timeOfDay),
                note = form.note,
            ),
        )
        updateState { copy(payment = null) }
    }

    private fun today(): LocalDate = clock.now().toLocalDateTime(zone).date
}
