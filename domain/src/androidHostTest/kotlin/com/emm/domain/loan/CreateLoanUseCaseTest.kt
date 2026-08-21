package com.emm.domain.loan

import com.emm.domain.shared.LoanId
import com.emm.domain.shared.Money
import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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

class CreateLoanUseCaseTest {

    private val loanRepository = mockk<LoanRepository>()
    private val idProvider = mockk<UniqueIdProvider>()

    private val lima = TimeZone.of("America/Lima")
    private val today = LocalDate(2026, Month.AUGUST, 11)
    private val clock = object : Clock {
        override fun now(): Instant = LocalDateTime(today, LocalTime(9, 0)).toInstant(lima)
    }

    private val useCase = CreateLoanUseCase(loanRepository, idProvider, clock, lima)

    private val lentAt = LocalDateTime(2026, Month.AUGUST, 10, 12, 0)

    private val anyInsert = LoanInsert(
        personName = "Juan Pérez",
        principal = Money(50_000L),
        interestBps = 1_000,
        note = "for the trip",
        lentAt = lentAt,
    )

    @Test
    fun `create should throw ValidationError when person name is blank`() = runTest {
        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(anyInsert.copy(personName = "   "))
        }

        assertEquals(ValidationCode.PersonRequired, ex.code)
        coVerify(exactly = 0) { loanRepository.create(any()) }
    }

    @Test
    fun `create should throw ValidationError when principal is zero`() = runTest {
        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(anyInsert.copy(principal = Money(0L)))
        }

        assertEquals(ValidationCode.AmountMustBePositive, ex.code)
        coVerify(exactly = 0) { loanRepository.create(any()) }
    }

    @Test
    fun `create should throw ValidationError when principal is negative`() = runTest {
        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(anyInsert.copy(principal = Money(-100L)))
        }

        assertEquals(ValidationCode.AmountMustBePositive, ex.code)
        coVerify(exactly = 0) { loanRepository.create(any()) }
    }

    @Test
    fun `create should throw ValidationError when interest is below the minimum`() = runTest {
        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(anyInsert.copy(interestBps = MIN_INTEREST_BPS - 1))
        }

        assertEquals(ValidationCode.InterestOutOfRange, ex.code)
        coVerify(exactly = 0) { loanRepository.create(any()) }
    }

    @Test
    fun `create should throw ValidationError when interest is above the maximum`() = runTest {
        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(anyInsert.copy(interestBps = MAX_INTEREST_BPS + 1))
        }

        assertEquals(ValidationCode.InterestOutOfRange, ex.code)
        coVerify(exactly = 0) { loanRepository.create(any()) }
    }

    @Test
    fun `create should reject a date in the future and persist nothing`() = runTest {
        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(anyInsert.copy(lentAt = LocalDateTime(2026, Month.AUGUST, 12, 0, 0)))
        }

        assertEquals(ValidationCode.DateInTheFuture, ex.code)
        coVerify(exactly = 0) { loanRepository.create(any()) }
    }

    @Test
    fun `create derives personKey and totalDue and trims the stored name`() = runTest {
        every { idProvider.id } returns "loan-1"
        coEvery { loanRepository.create(any()) } just Runs

        useCase(anyInsert.copy(personName = "  Juan Pérez  "))

        coVerify {
            loanRepository.create(
                match {
                    it.id == LoanId("loan-1") &&
                        it.personName == "Juan Pérez" &&
                        it.personKey == "juan perez" &&
                        it.totalDue == Money(55_000L)
                },
            )
        }
    }

    @Test
    fun `create should propagate DomainException from repository`() = runTest {
        every { idProvider.id } returns "loan-1"
        coEvery { loanRepository.create(any()) } throws DomainException.DatabaseError(RuntimeException("boom"))

        val ex = assertFailsWith<DomainException.DatabaseError> { useCase(anyInsert) }
        assertEquals("boom", ex.message)
    }
}
