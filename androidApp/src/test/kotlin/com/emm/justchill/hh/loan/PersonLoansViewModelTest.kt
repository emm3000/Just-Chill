package com.emm.justchill.hh.loan

import com.emm.domain.loan.DeleteLoanUseCase
import com.emm.domain.loan.Loan
import com.emm.domain.loan.LoanBalance
import com.emm.domain.loan.LoanPaymentInsert
import com.emm.domain.loan.LoanRepository
import com.emm.domain.loan.PaymentMethod
import com.emm.domain.loan.RegisterLoanPaymentUseCase
import com.emm.domain.shared.LoanId
import com.emm.domain.shared.Money
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import com.emm.justchill.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class PersonLoansViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val loanRepository = mockk<LoanRepository>()
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

    // Six defaulted params, each asserted independently by the mapping test; totalDue is
    // deliberately not derived from LoanMath here, so collapsing any of them weakens the proof.
    @Suppress("LongParameterList")
    private fun loanBalance(
        loanId: String = "loan-1",
        personName: String = "Ana",
        principal: Long = 100_000L,
        totalDue: Long = 110_000L,
        paidSoFar: Long = 10_000L,
        lentAt: String = "2026-08-10T12:00:00",
    ) = LoanBalance(
        loan = Loan(
            id = LoanId(loanId),
            personName = personName,
            personKey = "ana",
            principal = Money(principal),
            interestBps = 250,
            totalDue = Money(totalDue),
            note = "",
            lentAt = LocalDateTime.parse(lentAt),
        ),
        paidSoFar = Money(paidSoFar),
    )

    private fun viewModel(personKey: String = "ana") =
        PersonLoansViewModel(personKey, loanRepository, deleteLoan, registerLoanPayment, fixedClock, lima)

    @Test
    fun `loansWithBalance maps loans with their balances and reads the person's name off them`() = runTest {
        every { loanRepository.loansWithBalance("ana") } returns flowOf(listOf(loanBalance()))

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("Ana", state.personName)
        val row = state.loans.single()
        assertEquals("loan-1", row.loanId)
        assertEquals("S/ 1,000.00", row.principal)
        assertEquals("S/ 1,100.00", row.totalDue)
        assertEquals("S/ 100.00", row.paidSoFar)
        assertEquals("S/ 1,000.00", row.remaining)
        assertEquals("10 de agosto de 2026", row.readableLentAt)
    }

    @Test
    fun `personName survives its last loan being deleted`() = runTest {
        every { loanRepository.loansWithBalance("ana") } returns flowOf(listOf(loanBalance()), emptyList())

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("Ana", state.personName)
        assertTrue(state.loans.isEmpty())
    }

    @Test
    fun `loans reflects a new balance emission without any explicit refresh intent`() = runTest {
        every { loanRepository.loansWithBalance("ana") } returns flowOf(
            listOf(loanBalance(paidSoFar = 10_000L)),
            listOf(loanBalance(paidSoFar = 50_000L)),
        )

        val vm = viewModel()
        advanceUntilIdle()

        val row = vm.state.value.loans.single()
        assertEquals("S/ 500.00", row.paidSoFar)
        assertEquals("S/ 600.00", row.remaining)
    }

    @Test
    fun `OnAddPaymentClick opens the payment sheet seeded with the clicked loan id`() = runTest {
        every { loanRepository.loansWithBalance("ana") } returns flowOf(emptyList())
        val vm = viewModel()

        vm.onIntent(PersonLoansIntent.OnAddPaymentClick("loan-9"))
        advanceUntilIdle()

        val form = vm.state.value.payment
        checkNotNull(form) { "Expected the payment sheet to be open" }
        assertEquals("loan-9", form.loanId)
        assertEquals(today, form.today)
    }

    @Test
    fun `OnPaymentConfirm calls RegisterLoanPaymentUseCase with the entered fields and closes the sheet`() = runTest {
        every { loanRepository.loansWithBalance("ana") } returns flowOf(emptyList())
        coEvery { registerLoanPayment(any()) } returns Unit
        val vm = viewModel()
        advanceUntilIdle()

        val paymentDate = LocalDate(2026, Month.MARCH, 4)
        vm.onIntent(PersonLoansIntent.OnAddPaymentClick("loan-9"))
        vm.onIntent(PersonLoansIntent.OnPaymentAmountChange("400000"))
        vm.onIntent(PersonLoansIntent.OnPaymentMethodChange(PaymentMethod.Transfer))
        vm.onIntent(PersonLoansIntent.OnPaymentDateSelected(paymentDate))
        vm.onIntent(PersonLoansIntent.OnPaymentNoteChange("Abono parcial"))
        vm.onIntent(PersonLoansIntent.OnPaymentConfirm)
        advanceUntilIdle()

        val insert = slot<LoanPaymentInsert>()
        coVerify(exactly = 1) { registerLoanPayment(capture(insert)) }
        assertEquals(LoanId("loan-9"), insert.captured.loanId)
        assertEquals(Money(400_000L), insert.captured.amount)
        assertEquals(PaymentMethod.Transfer, insert.captured.method)
        assertEquals(LocalDateTime(paymentDate, LocalTime(14, 30)), insert.captured.paidAt)
        assertEquals("Abono parcial", insert.captured.note)
        assertNull(vm.state.value.payment)
    }

    @Test
    fun `OnPaymentConfirm with PaymentExceedsBalance emits ShowError, keeps the sheet open and persists nothing`() =
        runTest {
            every { loanRepository.loansWithBalance("ana") } returns flowOf(emptyList())
            val error = DomainException.ValidationError(
                "Payment of 999900 exceeds remaining balance of 100000",
                ValidationCode.PaymentExceedsBalance,
            )
            coEvery { registerLoanPayment(any()) } throws error
            val vm = viewModel()
            val effects = mutableListOf<PersonLoansEffect>()
            val job = launch { vm.effect.collect { effects.add(it) } }

            vm.onIntent(PersonLoansIntent.OnAddPaymentClick("loan-9"))
            vm.onIntent(PersonLoansIntent.OnPaymentAmountChange("999900"))
            vm.onIntent(PersonLoansIntent.OnPaymentConfirm)
            advanceUntilIdle()

            val showError = effects.filterIsInstance<PersonLoansEffect.ShowError>().firstOrNull()
            checkNotNull(showError) { "Expected ShowError effect but got: $effects" }
            assertEquals("El abono es mayor que lo que falta pagar", showError.message)
            assertEquals("loan-9", vm.state.value.payment?.loanId)
            assertEquals("999900", vm.state.value.payment?.amountDigits)
            coVerify(exactly = 1) { registerLoanPayment(any()) }
            job.cancel()
        }

    @Test
    fun `OnEditLoanClick emits NavigateToEditLoan with the clicked loan id`() = runTest {
        every { loanRepository.loansWithBalance("ana") } returns flowOf(emptyList())
        val vm = viewModel()
        val effects = mutableListOf<PersonLoansEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(PersonLoansIntent.OnEditLoanClick("loan-9"))
        advanceUntilIdle()

        assertTrue(effects.any { it == PersonLoansEffect.NavigateToEditLoan("loan-9") })
        job.cancel()
    }

    @Test
    fun `OnDeleteDismiss clears pendingDelete`() = runTest {
        every { loanRepository.loansWithBalance("ana") } returns flowOf(emptyList())
        val vm = viewModel()

        vm.onIntent(PersonLoansIntent.OnDeleteClick("loan-1"))
        vm.onIntent(PersonLoansIntent.OnDeleteDismiss)
        advanceUntilIdle()

        assertNull(vm.state.value.pendingDelete)
    }

    @Test
    fun `OnDeleteConfirm calls DeleteLoanUseCase with the pending loan id and clears pendingDelete`() = runTest {
        every { loanRepository.loansWithBalance("ana") } returns flowOf(emptyList())
        coEvery { deleteLoan(any()) } returns Unit
        val vm = viewModel()

        vm.onIntent(PersonLoansIntent.OnDeleteClick("loan-1"))
        vm.onIntent(PersonLoansIntent.OnDeleteConfirm)
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteLoan(LoanId("loan-1")) }
        assertNull(vm.state.value.pendingDelete)
    }

    @Test
    fun `OnDeleteConfirm with DeleteLoanUseCase throwing emits ShowError and clears pendingDelete`() = runTest {
        every { loanRepository.loansWithBalance("ana") } returns flowOf(emptyList())
        coEvery { deleteLoan(any()) } throws DomainException.NotFound("loan-ghost")
        val vm = viewModel()
        val effects = mutableListOf<PersonLoansEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(PersonLoansIntent.OnDeleteClick("loan-ghost"))
        vm.onIntent(PersonLoansIntent.OnDeleteConfirm)
        advanceUntilIdle()

        assertTrue(effects.any { it is PersonLoansEffect.ShowError })
        assertNull(vm.state.value.pendingDelete)
        job.cancel()
    }
}
