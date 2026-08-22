package com.emm.justchill.hh.loan

import com.emm.domain.loan.DeleteLoanUseCase
import com.emm.domain.loan.Loan
import com.emm.domain.loan.LoanPayment
import com.emm.domain.loan.LoanPaymentInsert
import com.emm.domain.loan.LoanPaymentRepository
import com.emm.domain.loan.LoanRepository
import com.emm.domain.loan.PaymentMethod
import com.emm.domain.loan.RegisterLoanPaymentUseCase
import com.emm.domain.shared.LoanId
import com.emm.domain.shared.LoanPaymentId
import com.emm.domain.shared.Money
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import com.emm.justchill.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class LoanDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val loanRepository = mockk<LoanRepository>()
    private val loanPaymentRepository = mockk<LoanPaymentRepository>()
    private val deleteLoan = mockk<DeleteLoanUseCase>()
    private val registerLoanPayment = mockk<RegisterLoanPaymentUseCase>()

    private val lima = TimeZone.of("America/Lima")
    private val today = LocalDate(2026, Month.AUGUST, 10)

    private class MovableClock(var instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    private fun instantAt(date: LocalDate, hour: Int, minute: Int): Instant = Instant.fromEpochMilliseconds(
        LocalDateTime(date, LocalTime(hour, minute)).toInstant(lima).toEpochMilliseconds(),
    )

    private val fixedClock = MovableClock(instantAt(today, hour = 14, minute = 30))

    private val loanIdValue = LoanId("loan-1")

    private val loan = Loan(
        id = loanIdValue,
        personName = "Ana",
        personKey = "ana",
        principal = Money(100_000L),
        interestBps = 1_000,
        totalDue = Money(110_000L),
        note = "Nota del préstamo",
        lentAt = LocalDateTime.parse("2026-08-01T12:00:00"),
    )

    private fun payment(id: String, amount: Long, note: String = "") = LoanPayment(
        id = LoanPaymentId(id),
        loanId = loanIdValue,
        amount = Money(amount),
        method = PaymentMethod.Cash,
        paidAt = LocalDateTime.parse("2026-08-05T09:00:00"),
        note = note,
    )

    private fun viewModel() = LoanDetailViewModel(
        "loan-1",
        loanRepository,
        loanPaymentRepository,
        deleteLoan,
        registerLoanPayment,
        fixedClock,
        lima,
    )

    @Test
    fun `payments follow byLoan and remaining is derived from their amounts`() = runTest {
        every { loanRepository.byId(loanIdValue) } returns flowOf(loan)
        every { loanPaymentRepository.byLoan(loanIdValue) } returns flowOf(
            listOf(payment("pay-1", amount = 30_000L), payment("pay-2", amount = 20_000L)),
        )

        val vm = viewModel()
        advanceUntilIdle()

        val summary = checkNotNull(vm.state.value.summary)
        assertEquals("Ana", summary.personName)
        assertEquals("S/ 500.00", summary.paidSoFar)
        assertEquals("S/ 600.00", summary.remaining)
        assertEquals(listOf("pay-1", "pay-2"), vm.state.value.payments.map { it.paymentId })
    }

    @Test
    fun `OnEditLoanClick emits NavigateToEditLoan`() = runTest {
        every { loanRepository.byId(loanIdValue) } returns flowOf(loan)
        every { loanPaymentRepository.byLoan(loanIdValue) } returns flowOf(emptyList())
        val vm = viewModel()
        val effects = mutableListOf<LoanDetailEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }
        advanceUntilIdle()

        vm.onIntent(LoanDetailIntent.OnEditLoanClick)
        advanceUntilIdle()

        assertTrue(effects.any { it == LoanDetailEffect.NavigateToEditLoan })
        job.cancel()
    }

    @Test
    fun `OnDeleteLoanConfirm calls DeleteLoanUseCase with the route's loan id and emits LoanDeleted`() = runTest {
        every { loanRepository.byId(loanIdValue) } returns flowOf(loan)
        every { loanPaymentRepository.byLoan(loanIdValue) } returns flowOf(emptyList())
        coEvery { deleteLoan(any()) } returns Unit
        val vm = viewModel()
        val effects = mutableListOf<LoanDetailEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }
        advanceUntilIdle()

        vm.onIntent(LoanDetailIntent.OnDeleteLoanClick)
        vm.onIntent(LoanDetailIntent.OnDeleteLoanConfirm)
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteLoan(loanIdValue) }
        assertTrue(effects.any { it == LoanDetailEffect.LoanDeleted })
        job.cancel()
    }

    @Test
    fun `a loan that vanishes from byId emits LoanDeleted instead of spinning forever`() = runTest {
        val loans = MutableStateFlow<Loan?>(loan)
        every { loanRepository.byId(loanIdValue) } returns loans
        every { loanPaymentRepository.byLoan(loanIdValue) } returns flowOf(emptyList())
        val vm = viewModel()
        val effects = mutableListOf<LoanDetailEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }
        advanceUntilIdle()

        loans.value = null
        advanceUntilIdle()

        assertEquals(listOf<LoanDetailEffect>(LoanDetailEffect.LoanDeleted), effects)
        job.cancel()
    }

    @Test
    fun `OnDeleteLoanConfirm emits LoanDeleted once even though its own soft delete re-emits null`() = runTest {
        val loans = MutableStateFlow<Loan?>(loan)
        every { loanRepository.byId(loanIdValue) } returns loans
        every { loanPaymentRepository.byLoan(loanIdValue) } returns flowOf(emptyList())
        coEvery { deleteLoan(loanIdValue) } coAnswers { loans.value = null }
        val vm = viewModel()
        val effects = mutableListOf<LoanDetailEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }
        advanceUntilIdle()

        vm.onIntent(LoanDetailIntent.OnDeleteLoanClick)
        vm.onIntent(LoanDetailIntent.OnDeleteLoanConfirm)
        advanceUntilIdle()

        assertEquals(1, effects.count { it == LoanDetailEffect.LoanDeleted })
        job.cancel()
    }

    @Test
    fun `OnDeleteLoanConfirm with DeleteLoanUseCase throwing emits ShowError and clears pendingDeleteLoan`() = runTest {
        every { loanRepository.byId(loanIdValue) } returns flowOf(loan)
        every { loanPaymentRepository.byLoan(loanIdValue) } returns flowOf(emptyList())
        coEvery { deleteLoan(any()) } throws DomainException.NotFound("loan-ghost")
        val vm = viewModel()
        val effects = mutableListOf<LoanDetailEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }
        advanceUntilIdle()

        vm.onIntent(LoanDetailIntent.OnDeleteLoanClick)
        vm.onIntent(LoanDetailIntent.OnDeleteLoanConfirm)
        advanceUntilIdle()

        val showError = effects.filterIsInstance<LoanDetailEffect.ShowError>().firstOrNull()
        checkNotNull(showError) { "Expected ShowError effect but got: $effects" }
        assertEquals("No encontré eso", showError.message)
        assertFalse(vm.state.value.pendingDeleteLoan)
        job.cancel()
    }

    @Test
    fun `OnDeleteLoanConfirm dispatched twice before the first resolves calls DeleteLoanUseCase once`() = runTest {
        every { loanRepository.byId(loanIdValue) } returns flowOf(loan)
        every { loanPaymentRepository.byLoan(loanIdValue) } returns flowOf(emptyList())
        val gate = CompletableDeferred<Unit>()
        coEvery { deleteLoan(any()) } coAnswers { gate.await() }
        val vm = viewModel()
        advanceUntilIdle()

        vm.onIntent(LoanDetailIntent.OnDeleteLoanClick)
        vm.onIntent(LoanDetailIntent.OnDeleteLoanConfirm)
        vm.onIntent(LoanDetailIntent.OnDeleteLoanConfirm)
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteLoan(any()) }
        gate.complete(Unit)
    }

    @Test
    fun `OnDeletePaymentConfirm calls LoanPaymentRepository delete with the pending id and closes the confirmation`() =
        runTest {
            every { loanRepository.byId(loanIdValue) } returns flowOf(loan)
            every { loanPaymentRepository.byLoan(loanIdValue) } returns flowOf(listOf(payment("pay-1", 30_000L)))
            coEvery { loanPaymentRepository.delete(any()) } returns Unit
            val vm = viewModel()
            advanceUntilIdle()

            vm.onIntent(LoanDetailIntent.OnDeletePaymentClick("pay-1"))
            vm.onIntent(LoanDetailIntent.OnDeletePaymentConfirm)
            advanceUntilIdle()

            coVerify(exactly = 1) { loanPaymentRepository.delete(LoanPaymentId("pay-1")) }
            assertNull(vm.state.value.pendingDeletePaymentId)
        }

    @Test
    fun `remaining follows the repository flow after a delete with no manual refresh`() = runTest {
        val payments = MutableStateFlow(listOf(payment("pay-1", 30_000L), payment("pay-2", 20_000L)))
        every { loanRepository.byId(loanIdValue) } returns flowOf(loan)
        every { loanPaymentRepository.byLoan(loanIdValue) } returns payments
        coEvery { loanPaymentRepository.delete(LoanPaymentId("pay-2")) } coAnswers {
            payments.update { list -> list.filterNot { it.id == LoanPaymentId("pay-2") } }
        }
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals("S/ 600.00", checkNotNull(vm.state.value.summary).remaining)

        vm.onIntent(LoanDetailIntent.OnDeletePaymentClick("pay-2"))
        vm.onIntent(LoanDetailIntent.OnDeletePaymentConfirm)
        advanceUntilIdle()

        assertEquals("S/ 800.00", checkNotNull(vm.state.value.summary).remaining)
        assertEquals(listOf("pay-1"), vm.state.value.payments.map { it.paymentId })
    }

    @Test
    fun `OnDeletePaymentConfirm with LoanPaymentRepository delete throwing emits ShowError with the Spanish message`() =
        runTest {
            every { loanRepository.byId(loanIdValue) } returns flowOf(loan)
            every { loanPaymentRepository.byLoan(loanIdValue) } returns flowOf(listOf(payment("pay-1", 30_000L)))
            coEvery { loanPaymentRepository.delete(any()) } throws DomainException.NotFound("loan-payment-ghost")
            val vm = viewModel()
            val effects = mutableListOf<LoanDetailEffect>()
            val job = launch { vm.effect.collect { effects.add(it) } }
            advanceUntilIdle()

            vm.onIntent(LoanDetailIntent.OnDeletePaymentClick("pay-1"))
            vm.onIntent(LoanDetailIntent.OnDeletePaymentConfirm)
            advanceUntilIdle()

            val showError = effects.filterIsInstance<LoanDetailEffect.ShowError>().firstOrNull()
            checkNotNull(showError) { "Expected ShowError effect but got: $effects" }
            assertEquals("No encontré eso", showError.message)
            job.cancel()
        }

    @Test
    fun `OnDeletePaymentConfirm dispatched twice before the first resolves calls delete once`() = runTest {
        every { loanRepository.byId(loanIdValue) } returns flowOf(loan)
        every { loanPaymentRepository.byLoan(loanIdValue) } returns flowOf(listOf(payment("pay-1", 30_000L)))
        val gate = CompletableDeferred<Unit>()
        coEvery { loanPaymentRepository.delete(any()) } coAnswers { gate.await() }
        val vm = viewModel()
        advanceUntilIdle()

        vm.onIntent(LoanDetailIntent.OnDeletePaymentClick("pay-1"))
        vm.onIntent(LoanDetailIntent.OnDeletePaymentConfirm)
        vm.onIntent(LoanDetailIntent.OnDeletePaymentConfirm)
        advanceUntilIdle()

        coVerify(exactly = 1) { loanPaymentRepository.delete(any()) }
        gate.complete(Unit)
    }

    @Test
    fun `OnPaymentConfirm calls RegisterLoanPaymentUseCase with the entered fields and the clock's time of day`() =
        runTest {
            every { loanRepository.byId(loanIdValue) } returns flowOf(loan)
            every { loanPaymentRepository.byLoan(loanIdValue) } returns flowOf(emptyList())
            coEvery { registerLoanPayment(any()) } returns Unit
            val vm = viewModel()
            advanceUntilIdle()

            val paymentDate = LocalDate(2026, Month.MARCH, 4)
            vm.onIntent(LoanDetailIntent.PaymentFormIntent.OnAddPaymentClick)
            vm.onIntent(LoanDetailIntent.PaymentFormIntent.OnPaymentAmountChange("400000"))
            vm.onIntent(LoanDetailIntent.PaymentFormIntent.OnPaymentMethodChange(PaymentMethod.Transfer))
            vm.onIntent(LoanDetailIntent.PaymentFormIntent.OnPaymentDateSelected(paymentDate))
            vm.onIntent(LoanDetailIntent.PaymentFormIntent.OnPaymentNoteChange("Abono parcial"))
            vm.onIntent(LoanDetailIntent.PaymentFormIntent.OnPaymentConfirm)
            advanceUntilIdle()

            val insert = slot<LoanPaymentInsert>()
            coVerify(exactly = 1) { registerLoanPayment(capture(insert)) }
            assertEquals(loanIdValue, insert.captured.loanId)
            assertEquals(Money(400_000L), insert.captured.amount)
            assertEquals(PaymentMethod.Transfer, insert.captured.method)
            // The date comes from the picked value, but the time of day comes from the injected
            // Clock/TimeZone (14:30, the fixture's fixedClock) — never from the picked date's midnight.
            assertEquals(LocalDateTime(paymentDate, LocalTime(14, 30)), insert.captured.paidAt)
            assertEquals("Abono parcial", insert.captured.note)
            assertNull(vm.state.value.payment)
        }

    @Test
    fun `OnPaymentConfirm with PaymentExceedsBalance emits ShowError and keeps the sheet open`() = runTest {
        every { loanRepository.byId(loanIdValue) } returns flowOf(loan)
        every { loanPaymentRepository.byLoan(loanIdValue) } returns flowOf(emptyList())
        val error = DomainException.ValidationError(
            "Payment of 999900 exceeds remaining balance of 100000",
            ValidationCode.PaymentExceedsBalance,
        )
        coEvery { registerLoanPayment(any()) } throws error
        val vm = viewModel()
        val effects = mutableListOf<LoanDetailEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }
        advanceUntilIdle()

        vm.onIntent(LoanDetailIntent.PaymentFormIntent.OnAddPaymentClick)
        vm.onIntent(LoanDetailIntent.PaymentFormIntent.OnPaymentAmountChange("999900"))
        vm.onIntent(LoanDetailIntent.PaymentFormIntent.OnPaymentConfirm)
        advanceUntilIdle()

        val showError = effects.filterIsInstance<LoanDetailEffect.ShowError>().firstOrNull()
        checkNotNull(showError) { "Expected ShowError effect but got: $effects" }
        assertEquals("El abono es mayor que lo que falta pagar", showError.message)
        assertEquals("999900", vm.state.value.payment?.amountDigits)
        coVerify(exactly = 1) { registerLoanPayment(any()) }
        job.cancel()
    }

    @Test
    fun `OnPaymentConfirm dispatched twice before the first resolves calls RegisterLoanPaymentUseCase once`() =
        runTest {
            every { loanRepository.byId(loanIdValue) } returns flowOf(loan)
            every { loanPaymentRepository.byLoan(loanIdValue) } returns flowOf(emptyList())
            val gate = CompletableDeferred<Unit>()
            coEvery { registerLoanPayment(any()) } coAnswers { gate.await() }
            val vm = viewModel()
            advanceUntilIdle()

            vm.onIntent(LoanDetailIntent.PaymentFormIntent.OnAddPaymentClick)
            vm.onIntent(LoanDetailIntent.PaymentFormIntent.OnPaymentAmountChange("400000"))
            vm.onIntent(LoanDetailIntent.PaymentFormIntent.OnPaymentConfirm)
            vm.onIntent(LoanDetailIntent.PaymentFormIntent.OnPaymentConfirm)
            advanceUntilIdle()

            coVerify(exactly = 1) { registerLoanPayment(any()) }
            gate.complete(Unit)
        }
}
