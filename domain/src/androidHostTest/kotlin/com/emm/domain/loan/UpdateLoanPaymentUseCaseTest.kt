package com.emm.domain.loan

import com.emm.domain.shared.LoanId
import com.emm.domain.shared.LoanPaymentId
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

class UpdateLoanPaymentUseCaseTest {

    private val loanRepository = mockk<LoanRepository>()
    private val loanPaymentRepository = mockk<LoanPaymentRepository>()

    private val lima = TimeZone.of("America/Lima")
    private val today = LocalDate(2026, Month.AUGUST, 11)
    private val clock = object : Clock {
        override fun now(): Instant = LocalDateTime(today, LocalTime(9, 0)).toInstant(lima)
    }

    private val useCase = UpdateLoanPaymentUseCase(loanRepository, loanPaymentRepository, clock, lima)

    private val loanId = LoanId("loan-1")
    private val paymentId = LoanPaymentId("payment-1")
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

    private fun existingPayment(amount: Long) = LoanPayment(
        id = paymentId,
        loanId = loanId,
        amount = Money(amount),
        method = PaymentMethod.Cash,
        paidAt = paidAt,
        note = "",
    )

    private val anyUpdate = LoanPaymentUpdate(
        id = paymentId,
        amount = Money(4_000L),
        method = PaymentMethod.Cash,
        paidAt = paidAt,
        note = "",
    )

    @Test
    fun `update should throw ValidationError when amount is zero`() = runTest {
        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(anyUpdate.copy(amount = Money(0L)))
        }

        assertEquals(ValidationCode.AmountMustBePositive, ex.code)
        coVerify(exactly = 0) { loanPaymentRepository.update(any()) }
    }

    @Test
    fun `update should reject a date in the future and persist nothing`() = runTest {
        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(anyUpdate.copy(paidAt = LocalDateTime(2026, Month.AUGUST, 12, 0, 0)))
        }

        assertEquals(ValidationCode.DateInTheFuture, ex.code)
        coVerify(exactly = 0) { loanPaymentRepository.update(any()) }
    }

    @Test
    fun `update should throw NotFound when the loan does not exist`() = runTest {
        every { loanPaymentRepository.byId(paymentId) } returns flowOf(existingPayment(amount = 4_000L))
        every { loanRepository.byId(loanId) } returns flowOf(null)

        assertFailsWith<DomainException.NotFound> { useCase(anyUpdate) }
        coVerify(exactly = 0) { loanPaymentRepository.update(any()) }
    }

    @Test
    fun `update should throw NotFound when the payment does not exist`() = runTest {
        every { loanPaymentRepository.byId(paymentId) } returns flowOf(null)

        assertFailsWith<DomainException.NotFound> { useCase(anyUpdate) }
        coVerify(exactly = 0) { loanPaymentRepository.update(any()) }
    }

    @Test
    fun `update rejects raising the only abono on a fully settled loan beyond totalDue`() = runTest {
        every { loanRepository.byId(loanId) } returns flowOf(loan)
        every { loanPaymentRepository.byId(paymentId) } returns flowOf(existingPayment(amount = 10_000L))
        coEvery { loanPaymentRepository.paidSoFar(loanId) } returns Money(10_000L)

        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(anyUpdate.copy(amount = Money(10_001L)))
        }

        assertEquals(ValidationCode.PaymentExceedsBalance, ex.code)
        coVerify(exactly = 0) { loanPaymentRepository.update(any()) }
    }

    @Test
    fun `update accepts lowering the only abono on a fully settled loan`() = runTest {
        every { loanRepository.byId(loanId) } returns flowOf(loan)
        every { loanPaymentRepository.byId(paymentId) } returns flowOf(existingPayment(amount = 10_000L))
        coEvery { loanPaymentRepository.paidSoFar(loanId) } returns Money(10_000L)
        coEvery { loanPaymentRepository.update(any()) } just Runs

        useCase(anyUpdate.copy(amount = Money(9_000L)))

        coVerify(exactly = 1) { loanPaymentRepository.update(match { it.amount == Money(9_000L) }) }
    }

    @Test
    fun `update accepts a payment that exactly settles what remains after excluding its own old amount`() = runTest {
        // Excluding this payment's old 4_000, the loan already has 2_000 paid elsewhere against a
        // 10_000 totalDue, leaving exactly 8_000 room for the new amount.
        every { loanRepository.byId(loanId) } returns flowOf(loan)
        every { loanPaymentRepository.byId(paymentId) } returns flowOf(existingPayment(amount = 4_000L))
        coEvery { loanPaymentRepository.paidSoFar(loanId) } returns Money(6_000L)
        coEvery { loanPaymentRepository.update(any()) } just Runs

        useCase(anyUpdate.copy(amount = Money(8_000L)))

        coVerify(exactly = 1) { loanPaymentRepository.update(match { it.amount == Money(8_000L) }) }
    }

    @Test
    fun `update rejects an amount exceeding remaining once its own old amount is excluded`() = runTest {
        // Two payments made this loan's totalDue (10_000): this one at 4_000, another at 6_000.
        // Excluding this one's old amount leaves 6_000 already paid, so remaining before it is 4_000.
        every { loanRepository.byId(loanId) } returns flowOf(loan)
        every { loanPaymentRepository.byId(paymentId) } returns flowOf(existingPayment(amount = 4_000L))
        coEvery { loanPaymentRepository.paidSoFar(loanId) } returns Money(10_000L)

        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(anyUpdate.copy(amount = Money(4_001L)))
        }

        assertEquals(ValidationCode.PaymentExceedsBalance, ex.code)
        coVerify(exactly = 0) { loanPaymentRepository.update(any()) }
    }

    @Test
    fun `update should propagate DomainException from repository`() = runTest {
        every { loanRepository.byId(loanId) } returns flowOf(loan)
        every { loanPaymentRepository.byId(paymentId) } returns flowOf(existingPayment(amount = 4_000L))
        coEvery { loanPaymentRepository.paidSoFar(loanId) } returns Money(6_000L)
        coEvery { loanPaymentRepository.update(any()) } throws DomainException.DatabaseError(RuntimeException("boom"))

        val ex = assertFailsWith<DomainException.DatabaseError> { useCase(anyUpdate) }
        assertEquals("boom", ex.message)
    }
}
