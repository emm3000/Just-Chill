package com.emm.justchill.feature.loan

import com.emm.justchill.core.domain.loan.CreateLoanUseCase
import com.emm.justchill.core.domain.loan.Loan
import com.emm.justchill.core.domain.loan.LoanInsert
import com.emm.justchill.core.domain.loan.LoanRepository
import com.emm.justchill.core.domain.loan.LoanUpdate
import com.emm.justchill.core.domain.loan.UpdateLoanUseCase
import com.emm.justchill.core.domain.shared.LoanId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.error.DomainException
import com.emm.justchill.core.domain.shared.error.ValidationCode
import com.emm.justchill.core.domain.time.TodayFlow
import com.emm.justchill.core.presentation.error.toUserMessage
import com.emm.justchill.core.presentation.format.centsToMoney
import com.emm.justchill.core.presentation.format.isSavableAmount
import com.emm.justchill.core.presentation.format.moneyCentsString
import com.emm.justchill.core.presentation.mvi.MviViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Suppress("LongParameterList")
class AddEditLoanViewModel(
    private val loanId: String?,
    private val loanRepository: LoanRepository,
    private val createLoan: CreateLoanUseCase,
    private val updateLoan: UpdateLoanUseCase,
    private val todayFlow: TodayFlow,
    // Only the hour a loan is stamped with comes from these; the day is TodayFlow's answer.
    private val clock: Clock,
    private val zone: TimeZone,
) : MviViewModel<AddEditLoanUiState, AddEditLoanIntent, AddEditLoanEffect>(
    AddEditLoanUiState(today = todayFlow.today(), isEdit = loanId != null),
) {

    // Set by loadLoan on the edit path; save() reuses its time-of-day so an edit never rewrites
    // lentAt to "now", which loansWithBalance and balancesByPerson both order and pick by.
    private var loadedLentAt: LocalDateTime? = null

    private var allPersonNames: List<String> = emptyList()

    init {
        loanRepository.balancesByPerson()
            .onEach { balances ->
                allPersonNames = balances.map { it.personName }.distinct().sorted()
                updateState { copy(personSuggestions = filterPersonSuggestions(personName)) }
            }
            .launchSafeIn(onError = { e -> AddEditLoanEffect.ShowError(e.toUserMessage()) })

        if (loanId != null) {
            launchSafe(onError = { e -> AddEditLoanEffect.ShowError(e.toUserMessage()) }) { loadLoan(loanId) }
        }
    }

    override fun onIntent(intent: AddEditLoanIntent) {
        updateState { copy(today = todayFlow.today()) }
        when (intent) {
            is AddEditLoanIntent.OnPersonNameChange -> {
                updateState {
                    copy(
                        personName = intent.value,
                        personSuggestions = filterPersonSuggestions(intent.value),
                    ).recalcSaveEnabled()
                }
            }

            is AddEditLoanIntent.OnAmountChange -> {
                updateState { copy(amountDigits = intent.digits, amountError = null).recalcSaveEnabled() }
            }

            is AddEditLoanIntent.OnInterestPercentChange -> {
                updateState {
                    copy(
                        interestPercentText = sanitizeInterestPercentInput(intent.value),
                        interestError = null,
                        amountError = null,
                    )
                }
            }

            is AddEditLoanIntent.OnDateSelected -> updateState { copy(date = intent.value) }

            is AddEditLoanIntent.OnNoteChange -> updateState { copy(note = intent.value) }

            AddEditLoanIntent.Save -> save()

            is AddEditLoanIntent.OnSheetRequested -> updateState { copy(openSheet = intent.sheet) }

            AddEditLoanIntent.OnSheetDismissed -> updateState { copy(openSheet = null) }
        }
    }

    private suspend fun loadLoan(id: String) {
        val loan: Loan? = loanRepository.byId(LoanId(id)).first()
        if (loan == null) {
            sendEffect(AddEditLoanEffect.NavigateBack)
            return
        }
        loadedLentAt = loan.lentAt
        updateState {
            copy(
                personName = loan.personName,
                personSuggestions = filterPersonSuggestions(loan.personName),
                amountDigits = moneyCentsString(loan.principal),
                interestPercentText = bpsToPercentText(loan.interestBps),
                date = loan.lentAt.date,
                note = loan.note,
            ).recalcSaveEnabled()
        }
    }

    private fun filterPersonSuggestions(personName: String): List<String> =
        allPersonNames.filter { it.contains(personName, ignoreCase = true) && it != personName }

    private fun save() = launchSafe(onError = ::refuse) {
        if (currentState.isSaving) return@launchSafe
        updateState { copy(isSaving = true) }
        val form: AddEditLoanUiState = currentState
        val timeOfDay: LocalTime = clock.now().toLocalDateTime(zone).time
        val lentAt = LocalDateTime(form.date ?: todayFlow.today(), loadedLentAt?.time ?: timeOfDay)
        val principal: Money = centsToMoney(form.amountDigits)
        val interestBps: Int = percentTextToBps(form.interestPercentText)
        val id: String? = loanId
        if (id != null) {
            updateLoan(
                LoanId(id),
                LoanUpdate(
                    personName = form.personName,
                    principal = principal,
                    interestBps = interestBps,
                    note = form.note,
                    lentAt = lentAt,
                ),
            )
        } else {
            createLoan(
                LoanInsert(
                    personName = form.personName,
                    principal = principal,
                    interestBps = interestBps,
                    note = form.note,
                    lentAt = lentAt,
                ),
            )
        }
        sendEffect(AddEditLoanEffect.NavigateBack)
    }

    private fun refuse(error: DomainException): AddEditLoanEffect? {
        val code: ValidationCode = (error as? DomainException.ValidationError)?.code ?: ValidationCode.Unspecified
        val message: String = error.toUserMessage()
        updateState { copy(isSaving = false) }
        return when (code) {
            ValidationCode.InterestOutOfRange -> {
                updateState { copy(interestError = message) }
                null
            }

            ValidationCode.TotalBelowPaid -> {
                updateState { copy(amountError = message) }
                null
            }

            else -> AddEditLoanEffect.ShowError(message)
        }
    }
}

private fun AddEditLoanUiState.recalcSaveEnabled(): AddEditLoanUiState =
    copy(isSaveEnabled = personName.isNotBlank() && amountDigits.isSavableAmount())
