package com.emm.justchill.hh.loan

import com.emm.domain.loan.DeleteLoanUseCase
import com.emm.domain.loan.Loan
import com.emm.domain.loan.LoanBalance
import com.emm.domain.loan.LoanRepository
import com.emm.domain.shared.LoanId
import com.emm.domain.shared.Money
import com.emm.domain.shared.error.DomainException
import com.emm.justchill.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PersonLoansViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val loanRepository = mockk<LoanRepository>()
    private val deleteLoan = mockk<DeleteLoanUseCase>()

    @Suppress("LongParameterList")
    private fun loanBalance(
        loanId: String = "loan-1",
        personName: String = "Ana",
        principal: Long = 100_000L,
        totalDue: Long = 110_000L,
        paidSoFar: Long = 10_000L,
        remaining: Long = 100_000L,
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
        remaining = Money(remaining),
    )

    private fun viewModel(personKey: String = "ana") = PersonLoansViewModel(personKey, loanRepository, deleteLoan)

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
