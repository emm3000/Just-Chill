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
import com.emm.justchill.hh.shared.formatNeutral
import com.emm.justchill.hh.shared.fromCentsToSolesWith
import com.emm.justchill.hh.transaction.centsToMoney
import com.emm.justchill.hh.transaction.moneyCentsString
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

// Eight independent dependencies, each already minimal: the loan id off the route, the two
// repositories the summary is combined from, the three use cases for the writes that carry
// invariants (cascade delete, payment validation on create and on edit), and Clock/TimeZone the
// graph binds by identity (AppGraphKoinTest) — folding any pair into a holder would lose that
// identity check, not the count.
@Suppress("LongParameterList")
class LoanDetailViewModel(
    private val loanId: String,
    loanRepository: LoanRepository,
    private val loanPaymentRepository: LoanPaymentRepository,
    private val deleteLoan: DeleteLoanUseCase,
    private val registerLoanPayment: RegisterLoanPaymentUseCase,
    private val updateLoanPayment: UpdateLoanPaymentUseCase,
    private val clock: Clock,
    private val zone: TimeZone,
) : MviViewModel<LoanDetailUiState, LoanDetailIntent, LoanDetailEffect>() {

    override val initialState = LoanDetailUiState()

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
                updateState { copy(summary = loanSummaryUi(loan, paidSoFar), payments = payments.toUi()) }
            }
            .launchIn(viewModelScope)
    }

    // Mirrors UpdateLoanPaymentUseCase's own remainingBeforeThis: the ceiling the sheet shows must
    // agree with what the use case actually enforces, so editing the only abono on a settled loan
    // shows its old amount as headroom instead of "Máximo S/ 0.00".
    private fun maxAmountLabel(excluding: Money): String? {
        val loan = loadedLoan ?: return null
        val paidSoFar = loadedPayments.fold(Money.Zero) { acc, payment -> acc + payment.amount }
        return formatNeutral(fromCentsToSolesWith(remaining(loan.totalDue, paidSoFar - excluding)))
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
            LoanDetailIntent.PaymentFormIntent.OnAddPaymentClick -> {
                updateState {
                    copy(
                        payment = LoanPaymentFormUi(
                            loanId = loanId,
                            today = today(),
                            maxAmountLabel = maxAmountLabel(excluding = Money.Zero),
                        ),
                    )
                }
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
                    today = today(),
                    amountDigits = moneyCentsString(payment.amount),
                    method = payment.method,
                    date = payment.paidAt.date,
                    note = payment.note,
                    editingPaymentId = payment.id.value,
                    originalPaidAt = payment.paidAt,
                    maxAmountLabel = maxAmountLabel(excluding = payment.amount),
                ),
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

    private fun today(): LocalDate = clock.now().toLocalDateTime(zone).date
}
