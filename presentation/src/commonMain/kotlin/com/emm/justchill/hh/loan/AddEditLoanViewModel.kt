package com.emm.justchill.hh.loan

import androidx.lifecycle.viewModelScope
import com.emm.domain.loan.CreateLoanUseCase
import com.emm.domain.loan.LoanInsert
import com.emm.domain.loan.LoanRepository
import com.emm.domain.loan.LoanUpdate
import com.emm.domain.loan.UpdateLoanUseCase
import com.emm.domain.shared.LoanId
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.hh.transaction.centsToMoney
import com.emm.justchill.hh.transaction.moneyCentsString
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class AddEditLoanViewModel(
    private val loanId: String?,
    private val loanRepository: LoanRepository,
    private val createLoan: CreateLoanUseCase,
    private val updateLoan: UpdateLoanUseCase,
    private val clock: Clock,
    private val zone: TimeZone,
) : MviViewModel<AddEditLoanUiState, AddEditLoanIntent, AddEditLoanEffect>() {

    override val initialState = AddEditLoanUiState(today = today(), isEdit = loanId != null)

    init {
        loanRepository.balancesByPerson()
            .onEach { balances ->
                val names = balances.map { it.personName }.distinct().sorted()
                updateState { copy(personSuggestions = names) }
            }
            .launchIn(viewModelScope)

        if (loanId != null) {
            viewModelScope.launch { loadLoan(loanId) }
        }
    }

    override fun onIntent(intent: AddEditLoanIntent) {
        when (intent) {
            is AddEditLoanIntent.OnPersonNameChange -> {
                updateState { copy(personName = intent.value).recalcSaveEnabled() }
            }

            is AddEditLoanIntent.OnPersonSuggestionSelected -> {
                updateState { copy(personName = intent.value).recalcSaveEnabled() }
            }

            is AddEditLoanIntent.OnAmountChange -> {
                updateState { copy(amountDigits = intent.digits).recalcSaveEnabled() }
            }

            is AddEditLoanIntent.OnInterestPercentChange -> updateState { copy(interestPercentText = intent.value) }

            is AddEditLoanIntent.OnDateSelected -> updateState { copy(date = intent.value) }

            is AddEditLoanIntent.OnNoteChange -> updateState { copy(note = intent.value) }

            AddEditLoanIntent.Save -> save()
        }
    }

    private suspend fun loadLoan(id: String) {
        val loan = loanRepository.byId(LoanId(id)).first() ?: return
        updateState {
            copy(
                personName = loan.personName,
                amountDigits = moneyCentsString(loan.principal),
                interestPercentText = bpsToPercentText(loan.interestBps),
                date = loan.lentAt.date,
                note = loan.note,
            ).recalcSaveEnabled()
        }
    }

    private fun save() = launchSafe(
        onError = { e -> AddEditLoanEffect.ShowError(e.toUserMessage()) },
    ) {
        val s = currentState
        val now: LocalDateTime = clock.now().toLocalDateTime(zone)
        val lentAt = LocalDateTime(s.date ?: now.date, now.time)
        val principal = centsToMoney(s.amountDigits)
        val interestBps = percentTextToBps(s.interestPercentText)
        val id = loanId
        if (id != null) {
            updateLoan(
                LoanId(id),
                LoanUpdate(
                    personName = s.personName,
                    principal = principal,
                    interestBps = interestBps,
                    note = s.note,
                    lentAt = lentAt,
                ),
            )
        } else {
            createLoan(
                LoanInsert(
                    personName = s.personName,
                    principal = principal,
                    interestBps = interestBps,
                    note = s.note,
                    lentAt = lentAt,
                ),
            )
        }
        sendEffect(AddEditLoanEffect.NavigateBack)
    }

    private fun today(): LocalDate = clock.now().toLocalDateTime(zone).date
}

private fun AddEditLoanUiState.recalcSaveEnabled(): AddEditLoanUiState =
    copy(isSaveEnabled = personName.isNotBlank() && amountDigits.isNotEmpty() && amountDigits.toLongOrNull() != 0L)
