package com.emm.justchill.hh.loan

import androidx.lifecycle.viewModelScope
import com.emm.domain.loan.DeleteLoanUseCase
import com.emm.domain.loan.Loan
import com.emm.domain.loan.LoanPayment
import com.emm.domain.loan.LoanPaymentInsert
import com.emm.domain.loan.LoanPaymentRepository
import com.emm.domain.loan.LoanPaymentUpdate
import com.emm.domain.loan.LoanRepository
import com.emm.domain.loan.RegisterLoanPaymentUseCase
import com.emm.domain.loan.UpdateLoanPaymentUseCase
import com.emm.domain.loan.remaining
import com.emm.domain.shared.LoanId
import com.emm.domain.shared.LoanPaymentId
import com.emm.domain.shared.Money
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.core.time.TodayFlow
import com.emm.justchill.hh.shared.formatNeutral
import com.emm.justchill.hh.shared.fromCentsToSolesWith
import com.emm.justchill.hh.transaction.centsToMoney
import com.emm.justchill.hh.transaction.moneyCentsString
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

// Nine independent dependencies, each already minimal: the loan id off the route, the two
// repositories the summary is combined from, the three use cases for the writes that carry
// invariants (cascade delete, payment validation on create and on edit), TodayFlow for the day a
// payment form opens on, and Clock/TimeZone — kept for the time of day only — which the graph binds
// by identity (AppGraphKoinTest); folding any pair into a holder would lose that identity check,
// not the count.
@Suppress("LongParameterList")
class LoanDetailViewModel(
    private val loanId: String,
    loanRepository: LoanRepository,
    private val loanPaymentRepository: LoanPaymentRepository,
    private val deleteLoan: DeleteLoanUseCase,
    private val registerLoanPayment: RegisterLoanPaymentUseCase,
    private val updateLoanPayment: UpdateLoanPaymentUseCase,
    private val todayFlow: TodayFlow,
    private val clock: Clock,
    private val zone: TimeZone,
) : MviViewModel<LoanDetailUiState, LoanDetailIntent, LoanDetailEffect>(LoanDetailUiState()) {

    // `byId` re-emits null for this ViewModel's own soft delete, so the delete path and the
    // vanished-loan path both reach the same exit; only the first one may pop the back stack.
    private var hasExited = false

    // The domain models behind `state.summary`/`state.payments`, kept for OnEditPaymentClick to
    // prefill the form and compute its amount ceiling — LoanSummaryUi/LoanPaymentRowUi only carry
    // already-formatted display strings, and Compose must not do money arithmetic on those.
    private var loadedLoan: Loan? = null
    private var loadedPayments: List<LoanPayment> = emptyList()

    init {
        combine(
            loanRepository.byId(LoanId(loanId)),
            loanPaymentRepository.byLoan(LoanId(loanId)),
        ) { loan, payments -> loan to payments }
            .onEach { (loan, payments) ->
                if (loan == null) {
                    exitDeletedLoan()
                    return@onEach
                }
                loadedLoan = loan
                loadedPayments = payments
                val paidSoFar = payments.fold(Money.Zero) { acc, payment -> acc + payment.amount }
                val nextSummary = loanSummaryUi(loan, paidSoFar)
                updateState {
                    copy(
                        summary = nextSummary,
                        payments = payments.toUi(),
                        // An open form's cap follows what is left — except mid-save, where the
                        // write's own re-emission would lower it under an abono that is winning.
                        payment = payment?.let { form -> if (form.isSaving) form else form.withCap() },
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    // The only place either cap field is set: every form is built capped at nothing and passed
    // through here, so one can never exist admitting more than the loan allows.
    //
    // Mirrors UpdateLoanPaymentUseCase's own remainingBeforeThis — editing the only abono on a
    // settled loan has its own old amount as headroom, not "Máximo S/ 0.00" — and fills the cents
    // the CTA compares against and the label the sheet shows from the same Money, so the two can
    // never disagree.
    private fun LoanPaymentFormUi.withCap(): LoanPaymentFormUi {
        val loan = loadedLoan ?: return this
        val edited = editingPaymentId?.let { id -> loadedPayments.find { it.id.value == id } }
        val paidSoFar = loadedPayments.fold(Money.Zero) { acc, payment -> acc + payment.amount }
        val ceiling = remaining(loan.totalDue, paidSoFar - (edited?.amount ?: Money.Zero))
        return copy(remainingCents = ceiling.cents, maxAmountLabel = formatNeutral(fromCentsToSolesWith(ceiling)))
    }

    override fun onIntent(intent: LoanDetailIntent) {
        when (intent) {
            LoanDetailIntent.OnEditLoanClick -> sendEffect(LoanDetailEffect.NavigateToEditLoan)

            LoanDetailIntent.OnDeleteLoanClick -> updateState { copy(pendingDeleteLoan = true) }

            LoanDetailIntent.OnDeleteLoanDismiss -> updateState { copy(pendingDeleteLoan = false) }

            LoanDetailIntent.OnDeleteLoanConfirm -> confirmDeleteLoan()

            is LoanDetailIntent.OnDeletePaymentClick -> {
                updateState { copy(pendingDeletePaymentId = intent.paymentId) }
            }

            LoanDetailIntent.OnDeletePaymentDismiss -> updateState { copy(pendingDeletePaymentId = null) }

            LoanDetailIntent.OnDeletePaymentConfirm -> confirmDeletePayment()

            is LoanDetailIntent.PaymentFormIntent -> onPaymentFormIntent(intent)
        }
    }

    private fun onPaymentFormIntent(intent: LoanDetailIntent.PaymentFormIntent) {
        when (intent) {
            LoanDetailIntent.PaymentFormIntent.OnAddPaymentClick -> updateState {
                val form = LoanPaymentFormUi(loanId = loanId, today = todayFlow.today(), remainingCents = 0L)
                copy(payment = form.withCap())
            }

            is LoanDetailIntent.PaymentFormIntent.OnEditPaymentClick -> onEditPaymentClick(intent.paymentId)

            is LoanDetailIntent.PaymentFormIntent.OnPaymentAmountChange -> {
                updatePayment { copy(amountDigits = intent.digits) }
            }

            is LoanDetailIntent.PaymentFormIntent.OnPaymentMethodChange -> {
                updatePayment { copy(method = intent.method) }
            }

            is LoanDetailIntent.PaymentFormIntent.OnPaymentDateSelected -> {
                updatePayment { copy(date = intent.value) }
            }

            is LoanDetailIntent.PaymentFormIntent.OnPaymentNoteChange -> {
                updatePayment { copy(note = intent.value) }
            }

            LoanDetailIntent.PaymentFormIntent.OnPaymentDismiss -> updateState { copy(payment = null) }

            LoanDetailIntent.PaymentFormIntent.OnPaymentConfirm -> confirmPayment()
        }
    }

    private fun updatePayment(reducer: LoanPaymentFormUi.() -> LoanPaymentFormUi) {
        updateState { copy(payment = payment?.reducer()) }
    }

    private fun onEditPaymentClick(paymentId: String) {
        val payment = loadedPayments.find { it.id.value == paymentId } ?: return
        updateState {
            copy(
                payment = LoanPaymentFormUi(
                    loanId = loanId,
                    today = todayFlow.today(),
                    remainingCents = 0L,
                    amountDigits = moneyCentsString(payment.amount),
                    method = payment.method,
                    date = payment.paidAt.date,
                    note = payment.note,
                    editingPaymentId = payment.id.value,
                    originalPaidAt = payment.paidAt,
                ).withCap(),
            )
        }
    }

    private fun confirmDeleteLoan() = launchSafe(
        onError = { e ->
            updateState { copy(isDeletingLoan = false, pendingDeleteLoan = false) }
            LoanDetailEffect.ShowError(e.toUserMessage())
        },
    ) {
        if (currentState.isDeletingLoan) return@launchSafe
        updateState { copy(isDeletingLoan = true) }
        deleteLoan(LoanId(loanId))
        exitDeletedLoan()
    }

    private fun exitDeletedLoan() {
        if (hasExited) return
        hasExited = true
        sendEffect(LoanDetailEffect.LoanDeleted)
    }

    private fun confirmDeletePayment() = launchSafe(
        onError = { e ->
            updateState { copy(isDeletingPayment = false, pendingDeletePaymentId = null) }
            LoanDetailEffect.ShowError(e.toUserMessage())
        },
    ) {
        if (currentState.isDeletingPayment) return@launchSafe
        val target = currentState.pendingDeletePaymentId ?: return@launchSafe
        updateState { copy(isDeletingPayment = true) }
        loanPaymentRepository.delete(LoanPaymentId(target))
        updateState { copy(pendingDeletePaymentId = null, isDeletingPayment = false) }
    }

    /**
     * Left open on failure, unlike confirmDeletePayment: the form still holds a typed amount worth
     * fixing, where the delete dialog has nothing left to edit.
     */
    private fun confirmPayment() = launchSafe(
        onError = { e ->
            updateState { copy(payment = payment?.copy(isSaving = false)) }
            LoanDetailEffect.ShowError(e.toUserMessage())
        },
    ) {
        val form = currentState.payment ?: return@launchSafe
        if (form.isSaving) return@launchSafe
        updateState { copy(payment = payment?.copy(isSaving = true)) }
        val editingId = form.editingPaymentId
        if (editingId == null) {
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
        } else {
            // Preserves the edited payment's original time-of-day: the date picker only ever
            // offers a date, and `loan_payments.byLoan` is `ORDER BY paidAt DESC`, so stamping
            // "now" here would silently reorder the abono list on every edit.
            val timeOfDay = form.originalPaidAt?.time ?: clock.now().toLocalDateTime(zone).time
            updateLoanPayment(
                LoanPaymentUpdate(
                    id = LoanPaymentId(editingId),
                    amount = centsToMoney(form.amountDigits),
                    method = form.method,
                    paidAt = LocalDateTime(form.date ?: form.today, timeOfDay),
                    note = form.note,
                ),
            )
        }
        updateState { copy(payment = null) }
    }
}
