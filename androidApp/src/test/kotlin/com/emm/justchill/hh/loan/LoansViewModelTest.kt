package com.emm.justchill.hh.loan

import com.emm.domain.loan.LoanRepository
import com.emm.domain.loan.PersonBalance
import com.emm.domain.shared.Money
import com.emm.justchill.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoansViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val loanRepository = mockk<LoanRepository>()

    @Test
    fun `balancesByPerson maps to PersonBalanceUi with the money formatted neutral`() = runTest {
        every { loanRepository.balancesByPerson() } returns flowOf(
            listOf(PersonBalance(personKey = "ana", personName = "Ana", remaining = Money(150_000L))),
        )

        val viewModel = LoansViewModel(loanRepository)
        advanceUntilIdle()

        val person = viewModel.state.value.people.single()
        assertEquals("ana", person.personKey)
        assertEquals("Ana", person.personName)
        assertEquals("S/ 1,500.00", person.remaining)
    }

    @Test
    fun `an empty ledger leaves people empty`() = runTest {
        every { loanRepository.balancesByPerson() } returns flowOf(emptyList())

        val viewModel = LoansViewModel(loanRepository)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.people.isEmpty())
    }

    @Test
    fun `OnPersonClick emits NavigateToPerson with the right personKey`() = runTest {
        every { loanRepository.balancesByPerson() } returns flowOf(emptyList())
        val viewModel = LoansViewModel(loanRepository)
        val effects = mutableListOf<LoansEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onIntent(LoansIntent.OnPersonClick("ana"))
        advanceUntilIdle()

        assertTrue(effects.any { it is LoansEffect.NavigateToPerson && it.personKey == "ana" })
        job.cancel()
    }
}
