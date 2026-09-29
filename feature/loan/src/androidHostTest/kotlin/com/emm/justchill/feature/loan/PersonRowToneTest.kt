package com.emm.justchill.feature.loan

import com.emm.justchill.core.domain.loan.LoanRepository
import com.emm.justchill.core.domain.loan.PersonBalance
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class PersonRowToneTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val loanRepository = mockk<LoanRepository>()

    private suspend fun toneOf(remainingCents: Long): PersonRemainingTone {
        every { loanRepository.balancesByPerson() } returns flowOf(
            listOf(PersonBalance(personKey = "ana", personName = "Ana", remaining = Money(remainingCents))),
        )
        val viewModel = LoansViewModel(loanRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        return viewModel.state.value.people.single().tone
    }

    @Test
    fun `a positive remaining takes success, same as LoansSection's total`() = runTest {
        assertEquals(PersonRemainingTone.Positive, toneOf(remainingCents = 150_000L))
    }

    @Test
    fun `a settled balance is the muted step, not success`() = runTest {
        assertEquals(PersonRemainingTone.Muted, toneOf(remainingCents = 0L))
    }

    @Test
    fun `an unsettled, non-positive remaining stays monochrome, not muted`() = runTest {
        assertEquals(PersonRemainingTone.Neutral, toneOf(remainingCents = -5_000L))
    }
}
