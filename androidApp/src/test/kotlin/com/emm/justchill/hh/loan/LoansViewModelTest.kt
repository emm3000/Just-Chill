package com.emm.justchill.hh.loan

import com.emm.domain.loan.LoanRepository
import com.emm.domain.loan.PersonBalance
import com.emm.domain.shared.Money
import com.emm.domain.shared.error.DomainException
import com.emm.justchill.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
    fun `an empty ledger leaves people empty, then reflects a later push`() = runTest {
        val balances = MutableStateFlow<List<PersonBalance>>(emptyList())
        every { loanRepository.balancesByPerson() } returns balances

        val viewModel = LoansViewModel(loanRepository)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.people.isEmpty())

        balances.value = listOf(PersonBalance(personKey = "ana", personName = "Ana", remaining = Money(150_000L)))
        advanceUntilIdle()

        assertEquals("Ana", viewModel.state.value.people.single().personName)
    }

    @Test
    fun `a failed balancesByPerson read emits ShowError instead of leaving the list empty`() = runTest {
        every { loanRepository.balancesByPerson() } returns
            flow { throw DomainException.DatabaseError(RuntimeException("disk full")) }
        val viewModel = LoansViewModel(loanRepository)
        val effects = mutableListOf<LoansEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        advanceUntilIdle()

        assertTrue(effects.any { it is LoansEffect.ShowError })
        job.cancel()
    }

    @Test
    fun `a locked database is retried, and the same instance keeps taking pushes afterwards`() = runTest {
        val balances = MutableStateFlow<List<PersonBalance>>(emptyList())
        var subscriptions = 0
        every { loanRepository.balancesByPerson() } returns flow {
            subscriptions++
            if (subscriptions == 1) throw DomainException.DatabaseError(RuntimeException("database is locked"))
            emitAll(balances)
        }
        val viewModel = LoansViewModel(loanRepository)
        val effects = mutableListOf<LoansEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }
        advanceUntilIdle()

        assertTrue(effects.isEmpty(), "a failure the retry absorbs never reaches the user")

        // No new LoansViewModel here on purpose: the screen the user came back to holds this one.
        balances.value = listOf(PersonBalance(personKey = "ana", personName = "Ana", remaining = Money(150_000L)))
        advanceUntilIdle()

        assertEquals("Ana", viewModel.state.value.people.single().personName)
        job.cancel()
    }

    @Test
    fun `OnAddLoanClick emits NavigateToAddLoan`() = runTest {
        every { loanRepository.balancesByPerson() } returns flowOf(emptyList())
        val viewModel = LoansViewModel(loanRepository)
        val effects = mutableListOf<LoansEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onIntent(LoansIntent.OnAddLoanClick)
        advanceUntilIdle()

        assertTrue(effects.any { it is LoansEffect.NavigateToAddLoan })
        job.cancel()
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
