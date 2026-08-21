package com.emm.domain.loan

import com.emm.domain.shared.LoanId
import com.emm.domain.shared.Money
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
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

    private val lima = TimeZone.of("America/Lima")
    private val today = LocalDate(2026, Month.AUGUST, 11)
    private val clock = object : Clock {
        override fun now(): Instant = LocalDateTime(today, LocalTime(9, 0)).toInstant(lima)
    }

    private val useCase = UpdateLoanUseCase(loanRepository, clock, lima)

    private val loanId = LoanId("loan-1")
    private val lentAt = LocalDateTime(2026, Month.AUGUST, 10, 12, 0)

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
    fun `update recomputes totalDue instead of trusting the caller's stale value`() = runTest {
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
        coEvery { loanRepository.update(any()) } just Runs

        useCase(loanId, anyUpdate.copy(personName = "  Muñoz  "))

        coVerify {
            loanRepository.update(
                match { it.personName == "Muñoz" && it.personKey == "munoz" },
            )
        }
    }

    @Test
    fun `update should propagate DomainException from repository`() = runTest {
        coEvery { loanRepository.update(any()) } throws DomainException.NotFound("Loan")

        assertFailsWith<DomainException.NotFound> { useCase(loanId, anyUpdate) }
    }
}
