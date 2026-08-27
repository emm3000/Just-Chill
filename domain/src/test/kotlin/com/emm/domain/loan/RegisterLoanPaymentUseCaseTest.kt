package com.emm.domain.loan

import com.emm.domain.shared.LoanId
import com.emm.domain.shared.LoanPaymentId
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

class RegisterLoanPaymentUseCaseTest {

    private val loanRepository = mockk<LoanRepository>()
    private val loanPaymentRepository = mockk<LoanPaymentRepository>()
    private val idProvider = mockk<UniqueIdProvider>()

    private val lima = TimeZone.of("America/Lima")
    private val today = LocalDate(2026, Month.AUGUST, 11)
    private val clock = object : Clock {
        override fun now(): Instant = LocalDateTime(today, LocalTime(9, 0)).toInstant(lima)
    }

    private val useCase = RegisterLoanPaymentUseCase(loanRepository, loanPaymentRepository, idProvider, clock, lima)

    private val loanId = LoanId("loan-1")
    private val paidAt = LocalDateTime(2026, Month.AUGUST, 10, 12, 0)

    private val loan = Loan(
        id = loanId,
        personName = "Juan Pérez",
        personKey = "juan perez",
        principal = Money(10_000L),
        interestBps = 0,
        totalDue = Money(10_000L),
        note = "",
        lentAt = paidAt,
    )

    private val anyInsert = LoanPaymentInsert(
        loanId = loanId,
        amount = Money(4_000L),
        method = PaymentMethod.Cash,
        paidAt = paidAt,
        note = "",
    )

    @Test
    fun `register should throw ValidationError when amount is zero`() = runTest {
        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(anyInsert.copy(amount = Money(0L)))
        }

        assertEquals(ValidationCode.AmountMustBePositive, ex.code)
        coVerify(exactly = 0) { loanPaymentRepository.create(any()) }
    }

    @Test
    fun `register should reject a date in the future and persist nothing`() = runTest {
        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(anyInsert.copy(paidAt = LocalDateTime(2026, Month.AUGUST, 12, 0, 0)))
        }

        assertEquals(ValidationCode.DateInTheFuture, ex.code)
        coVerify(exactly = 0) { loanPaymentRepository.create(any()) }
    }

    @Test
    fun `register should throw NotFound when the loan does not exist`() = runTest {
        every { loanRepository.byId(loanId) } returns flowOf(null)

        assertFailsWith<DomainException.NotFound> { useCase(anyInsert) }
        coVerify(exactly = 0) { loanPaymentRepository.create(any()) }
    }

    @Test
    fun `register should throw PaymentExceedsBalance when the amount is more than what remains`() = runTest {
        every { loanRepository.byId(loanId) } returns flowOf(loan)
        coEvery { loanPaymentRepository.paidSoFar(loanId) } returns Money(8_000L)

        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(anyInsert.copy(amount = Money(3_000L)))
        }

        assertEquals(ValidationCode.PaymentExceedsBalance, ex.code)
        coVerify(exactly = 0) { loanPaymentRepository.create(any()) }
    }

    @Test
    fun `register accepts a payment that exactly settles what remains`() = runTest {
        every { loanRepository.byId(loanId) } returns flowOf(loan)
        coEvery { loanPaymentRepository.paidSoFar(loanId) } returns Money(6_000L)
        coEvery { loanPaymentRepository.create(any()) } just Runs
        every { idProvider.id } returns "payment-1"

        useCase(anyInsert.copy(amount = Money(4_000L)))

        coVerify(exactly = 1) {
            loanPaymentRepository.create(
                match { it.id == LoanPaymentId("payment-1") && it.amount == Money(4_000L) },
            )
        }
    }

    @Test
    fun `register should propagate DomainException from repository`() = runTest {
        every { loanRepository.byId(loanId) } returns flowOf(loan)
        coEvery { loanPaymentRepository.paidSoFar(loanId) } returns Money.Zero
        every { idProvider.id } returns "payment-1"
        coEvery { loanPaymentRepository.create(any()) } throws DomainException.DatabaseError(RuntimeException("boom"))

        val ex = assertFailsWith<DomainException.DatabaseError> { useCase(anyInsert) }
        assertEquals("boom", ex.message)
    }
}
