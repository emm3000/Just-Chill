package com.emm.justchill.hh.loan

import com.emm.domain.loan.CreateLoanUseCase
import com.emm.domain.loan.Loan
import com.emm.domain.loan.LoanInsert
import com.emm.domain.loan.LoanRepository
import com.emm.domain.loan.LoanUpdate
import com.emm.domain.loan.UpdateLoanUseCase
import com.emm.domain.shared.LoanId
import com.emm.domain.shared.Money
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import com.emm.justchill.MainDispatcherRule
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.time.FakeTodayFlow
import com.emm.justchill.hh.transaction.moneyCentsString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

class AddEditLoanViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val lima = TimeZone.of("America/Lima")
    private val today = LocalDate(2026, Month.AUGUST, 10)

    private class MovableClock(var instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    private fun instantAt(date: LocalDate, hour: Int, minute: Int): Instant = Instant.fromEpochMilliseconds(
        LocalDateTime(date, LocalTime(hour, minute)).toInstant(lima).toEpochMilliseconds(),
    )

    private val fixedClock = MovableClock(instantAt(today, hour = 14, minute = 30))
    private val todayDates = MutableStateFlow(today)

    private val loanRepository = mockk<LoanRepository> {
        every { balancesByPerson() } returns flowOf(emptyList())
    }
    private val createLoan = mockk<CreateLoanUseCase>(relaxed = true)
    private val updateLoan = mockk<UpdateLoanUseCase>(relaxed = true)

    /** Recorded on 4 March 2026 at 09:15:33 — a time the fixed clock (14:30) never produces. */
    private val marchDay = LocalDate(2026, Month.MARCH, 4)
    private val marchLentAt = LocalDateTime(marchDay, LocalTime(9, 15, 33))

    private val storedLoan = Loan(
        id = LoanId("loan-1"),
        personName = "Ana",
        personKey = "ana",
        principal = Money(150_000L),
        interestBps = 750,
        totalDue = Money(161_250L),
        note = "Prestado en efectivo",
        lentAt = marchLentAt,
    )

    private fun viewModel(loanId: String? = null) = AddEditLoanViewModel(
        loanId = loanId,
        loanRepository = loanRepository,
        createLoan = createLoan,
        updateLoan = updateLoan,
        todayFlow = FakeTodayFlow(todayDates),
        clock = fixedClock,
        zone = lima,
    )

    /**
     * The Clock this ViewModel still holds answers "what hour", never "what day" — neither for the
     * initial state nor for the lentAt a save stamps. The two tests below pin one each, by
     * pointing TodayFlow at a day the clock does not agree with.
     */
    @Test
    fun `today is TodayFlow's day, not the clock's`() = runTest {
        val christmas = LocalDate(2026, Month.DECEMBER, 25)
        todayDates.value = christmas

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(christmas, vm.state.value.today)
    }

    @Test
    fun `an untouched date is lent on TodayFlow's day, at the clock's hour`() = runTest {
        coEvery { createLoan(any()) } returns Unit
        // The two disagree on purpose: the day must come from TodayFlow and the hour from the
        // clock, which is the only split that tells a second date derivation apart from none.
        val christmas = LocalDate(2026, Month.DECEMBER, 25)
        todayDates.value = christmas
        val vm = viewModel()

        vm.onIntent(AddEditLoanIntent.OnPersonNameChange("Ana"))
        vm.onIntent(AddEditLoanIntent.OnAmountChange("150000"))
        vm.onIntent(AddEditLoanIntent.Save)
        advanceUntilIdle()

        val insert = slot<LoanInsert>()
        coVerify { createLoan(capture(insert)) }
        assertEquals(LocalDateTime(christmas, LocalTime(14, 30)), insert.captured.lentAt)
    }

    @Test
    fun `create with CreateLoanUseCase throwing PersonRequired emits ShowError with the Spanish message`() = runTest {
        val error = DomainException.ValidationError("Person is required", ValidationCode.PersonRequired)
        coEvery { createLoan(any()) } throws error
        val vm = viewModel()
        val effects = mutableListOf<AddEditLoanEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(AddEditLoanIntent.Save)
        advanceUntilIdle()

        val showError = effects.filterIsInstance<AddEditLoanEffect.ShowError>().firstOrNull()
        checkNotNull(showError) { "Expected ShowError effect but got: $effects" }
        assertEquals(error.toUserMessage(), showError.message)
        assertEquals("Escribe a quién le prestaste", showError.message)
        assertFalse(showError.message.contains("required"), "must not leak the English diagnostic message")
        job.cancel()
    }

    @Test
    fun `create with CreateLoanUseCase throwing InterestOutOfRange emits ShowError with the Spanish message`() =
        runTest {
            val error = DomainException.ValidationError(
                "Interest must be between 0 and 10000 bps, got 50000",
                ValidationCode.InterestOutOfRange,
            )
            coEvery { createLoan(any()) } throws error
            val vm = viewModel()
            val effects = mutableListOf<AddEditLoanEffect>()
            val job = launch { vm.effect.collect { effects.add(it) } }

            vm.onIntent(AddEditLoanIntent.Save)
            advanceUntilIdle()

            val showError = effects.filterIsInstance<AddEditLoanEffect.ShowError>().firstOrNull()
            checkNotNull(showError) { "Expected ShowError effect but got: $effects" }
            assertEquals(error.toUserMessage(), showError.message)
            assertEquals("El interés debe estar entre 0% y 100%", showError.message)
            assertFalse(showError.message.contains("bps"), "must not leak the English diagnostic message")
            job.cancel()
        }

    @Test
    fun `a null loanId calls CreateLoanUseCase and never UpdateLoanUseCase`() = runTest {
        val vm = viewModel(loanId = null)

        vm.onIntent(AddEditLoanIntent.Save)
        advanceUntilIdle()

        coVerify(exactly = 1) { createLoan(any()) }
        coVerify(exactly = 0) { updateLoan(any(), any()) }
    }

    @Test
    fun `Save dispatched twice before the first resolves calls CreateLoanUseCase once`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { createLoan(any()) } coAnswers { gate.await() }
        val vm = viewModel()

        vm.onIntent(AddEditLoanIntent.Save)
        vm.onIntent(AddEditLoanIntent.Save)
        advanceUntilIdle()

        coVerify(exactly = 1) { createLoan(any()) }
        gate.complete(Unit)
    }

    @Test
    fun `a non-null loanId calls UpdateLoanUseCase with that LoanId and never CreateLoanUseCase`() = runTest {
        every { loanRepository.byId(LoanId("loan-1")) } returns flowOf(storedLoan)
        val vm = viewModel(loanId = "loan-1")
        advanceUntilIdle()

        vm.onIntent(AddEditLoanIntent.Save)
        advanceUntilIdle()

        coVerify(exactly = 1) { updateLoan(LoanId("loan-1"), any()) }
        coVerify(exactly = 0) { createLoan(any()) }
    }

    @Test
    fun `loadLoan seeds every field from the loaded loan`() = runTest {
        every { loanRepository.byId(LoanId("loan-1")) } returns flowOf(storedLoan)
        val vm = viewModel(loanId = "loan-1")
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("Ana", state.personName)
        assertEquals(moneyCentsString(storedLoan.principal), state.amountDigits)
        // 750 bps == 7.50%; bpsToPercentText is internal to :presentation and unreachable here.
        assertEquals("7.50", state.interestPercentText)
        assertEquals(marchDay, state.date)
        assertEquals("Prestado en efectivo", state.note)
    }

    @Test
    fun `a loanId whose loan no longer exists emits NavigateBack instead of an empty edit form`() = runTest {
        every { loanRepository.byId(LoanId("loan-gone")) } returns flowOf(null)
        val vm = viewModel(loanId = "loan-gone")
        val effects = mutableListOf<AddEditLoanEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }
        advanceUntilIdle()

        assertEquals(listOf<AddEditLoanEffect>(AddEditLoanEffect.NavigateBack), effects)
        job.cancel()
    }

    @Test
    fun `an interest-only edit preserves the loaded lentAt time-of-day`() = runTest {
        every { loanRepository.byId(LoanId("loan-1")) } returns flowOf(storedLoan)
        val vm = viewModel(loanId = "loan-1")
        advanceUntilIdle()

        vm.onIntent(AddEditLoanIntent.OnInterestPercentChange("15"))
        vm.onIntent(AddEditLoanIntent.Save)
        advanceUntilIdle()

        val update = slot<LoanUpdate>()
        coVerify { updateLoan(LoanId("loan-1"), capture(update)) }
        assertEquals(marchLentAt, update.captured.lentAt)
    }

    @Test
    fun `OnSheetRequested opens the requested sheet and OnSheetDismissed closes it`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertNull(vm.state.value.openSheet)

        vm.onIntent(AddEditLoanIntent.OnSheetRequested(LoanFormSheet.Amount))
        advanceUntilIdle()
        assertEquals(LoanFormSheet.Amount, vm.state.value.openSheet)

        vm.onIntent(AddEditLoanIntent.OnSheetDismissed)
        advanceUntilIdle()
        assertNull(vm.state.value.openSheet)
    }

    @Test
    fun `only one sheet is ever open — requesting a second replaces the first`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onIntent(AddEditLoanIntent.OnSheetRequested(LoanFormSheet.Amount))
        vm.onIntent(AddEditLoanIntent.OnSheetRequested(LoanFormSheet.Date))
        advanceUntilIdle()

        assertEquals(LoanFormSheet.Date, vm.state.value.openSheet)
    }
}
