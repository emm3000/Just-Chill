package com.emm.domain.loan

import com.emm.domain.shared.LoanId
import com.emm.domain.shared.Money
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock
import kotlin.time.Instant

class UpdateLoanUseCaseTest {

    private val loanRepository = mockk<LoanRepository>()
    private val loanPaymentRepository = mockk<LoanPaymentRepository>()

    private val lima = TimeZone.of("America/Lima")
    private val today = LocalDate(2026, Month.AUGUST, 11)
    private val clock = object : Clock {
        override fun now(): Instant = LocalDateTime(today, LocalTime(9, 0)).toInstant(lima)
    }

    private val useCase = UpdateLoanUseCase(loanRepository, loanPaymentRepository, clock, lima)

    private val loanId = LoanId("loan-1")
    private val lentAt = LocalDateTime(2026, Month.AUGUST, 10, 12, 0)

    private val loan = Loan(
        id = loanId,
        personName = "Juan Pérez",
        personKey = "juan perez",
        principal = Money(50_000L),
        interestBps = 0,
        totalDue = Money(50_000L),
        note = "",
        lentAt = lentAt,
    )

    private val anyUpdate = LoanUpdate(
        personName = "Juan Pérez",
        principal = Money(50_000L),
        interestBps = 1_000,
        note = "for the trip",
        lentAt = lentAt,
    )

    @Test
    fun `update should throw ValidationError when person name is blank`() = runTest {
        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(loanId, anyUpdate.copy(personName = "   "))
        }

        assertEquals(ValidationCode.PersonRequired, ex.code)
        coVerify(exactly = 0) { loanRepository.update(any()) }
    }

    @Test
    fun `update should throw ValidationError when principal is zero`() = runTest {
        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(loanId, anyUpdate.copy(principal = Money(0L)))
        }

        assertEquals(ValidationCode.AmountMustBePositive, ex.code)
        coVerify(exactly = 0) { loanRepository.update(any()) }
    }

    @Test
    fun `update should throw ValidationError when interest is out of range`() = runTest {
        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(loanId, anyUpdate.copy(interestBps = MAX_INTEREST_BPS + 1))
        }

        assertEquals(ValidationCode.InterestOutOfRange, ex.code)
        coVerify(exactly = 0) { loanRepository.update(any()) }
    }

    @Test
    fun `update should reject a date in the future and persist nothing`() = runTest {
        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(loanId, anyUpdate.copy(lentAt = LocalDateTime(2026, Month.AUGUST, 12, 0, 0)))
        }

        assertEquals(ValidationCode.DateInTheFuture, ex.code)
        coVerify(exactly = 0) { loanRepository.update(any()) }
    }

    @Test
    fun `update should throw NotFound when the loan does not exist`() = runTest {
        every { loanRepository.byId(loanId) } returns flowOf(null)

        assertFailsWith<DomainException.NotFound> { useCase(loanId, anyUpdate) }
        coVerify(exactly = 0) { loanRepository.update(any()) }
    }

    @Test
    fun `update recomputes totalDue instead of trusting the caller's stale value`() = runTest {
        every { loanRepository.byId(loanId) } returns flowOf(loan)
        coEvery { loanPaymentRepository.paidSoFar(loanId) } returns Money.Zero
        coEvery { loanRepository.update(any()) } just Runs

        useCase(loanId, anyUpdate.copy(principal = Money(20_000L), interestBps = 500))

        coVerify {
            loanRepository.update(
                match {
                    it.id == loanId &&
                        it.principal == Money(20_000L) &&
                        it.interestBps == 500 &&
                        it.totalDue == Money(21_000L)
                },
            )
        }
    }

    @Test
    fun `update derives personKey and trims the stored name`() = runTest {
        every { loanRepository.byId(loanId) } returns flowOf(loan)
        coEvery { loanPaymentRepository.paidSoFar(loanId) } returns Money.Zero
        coEvery { loanRepository.update(any()) } just Runs

        useCase(loanId, anyUpdate.copy(personName = "  Muñoz  "))

        coVerify {
            loanRepository.update(
                match { it.personName == "Muñoz" && it.personKey == "munoz" },
            )
        }
    }

    @Test
    fun `update throws TotalBelowPaid when the edited total is below what has already been paid`() = runTest {
        // principal = 50_000, interestBps = 0 -> totalDue = 50_000; 40_000 already paid.
        every { loanRepository.byId(loanId) } returns flowOf(loan)
        coEvery { loanPaymentRepository.paidSoFar(loanId) } returns Money(40_000L)

        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(loanId, anyUpdate.copy(principal = Money(30_000L), interestBps = 0))
        }

        assertEquals(ValidationCode.TotalBelowPaid, ex.code)
        coVerify(exactly = 0) { loanRepository.update(any()) }
    }

    @Test
    fun `update allows an edited total that exactly equals what has already been paid`() = runTest {
        every { loanRepository.byId(loanId) } returns flowOf(loan)
        coEvery { loanPaymentRepository.paidSoFar(loanId) } returns Money(40_000L)
        coEvery { loanRepository.update(any()) } just Runs

        useCase(loanId, anyUpdate.copy(principal = Money(40_000L), interestBps = 0))

        coVerify(exactly = 1) { loanRepository.update(match { it.totalDue == Money(40_000L) }) }
    }

    @Test
    fun `update should propagate DomainException from repository`() = runTest {
        every { loanRepository.byId(loanId) } returns flowOf(loan)
        coEvery { loanPaymentRepository.paidSoFar(loanId) } returns Money.Zero
        coEvery { loanRepository.update(any()) } throws DomainException.DatabaseError(RuntimeException("nope"))

        assertFailsWith<DomainException.DatabaseError> { useCase(loanId, anyUpdate) }
    }
}
