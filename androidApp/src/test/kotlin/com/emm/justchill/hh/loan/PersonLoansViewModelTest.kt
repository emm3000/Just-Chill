package com.emm.justchill.hh.loan

import com.emm.domain.loan.Loan
import com.emm.domain.loan.LoanBalance
import com.emm.domain.loan.LoanRepository
import com.emm.domain.shared.LoanId
import com.emm.domain.shared.Money
import com.emm.justchill.MainDispatcherRule
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
import kotlin.test.assertTrue

class PersonLoansViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val loanRepository = mockk<LoanRepository>()

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

    private fun viewModel(personKey: String = "ana") = PersonLoansViewModel(personKey, loanRepository)

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
    fun `OnLoanClick emits NavigateToLoanDetail with the clicked loan id`() = runTest {
        every { loanRepository.loansWithBalance("ana") } returns flowOf(emptyList())
        val vm = viewModel()
        val effects = mutableListOf<PersonLoansEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(PersonLoansIntent.OnLoanClick("loan-9"))
        advanceUntilIdle()

        assertTrue(effects.any { it == PersonLoansEffect.NavigateToLoanDetail("loan-9") })
        job.cancel()
    }
}
