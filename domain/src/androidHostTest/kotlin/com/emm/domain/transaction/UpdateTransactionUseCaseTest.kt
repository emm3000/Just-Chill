package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.DateAndTimeCombiner
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock
import kotlin.time.Instant

class UpdateTransactionUseCaseTest {

    private val repository = mockk<TransactionRepository>()
    private val dateAndTimeCombiner = mockk<DateAndTimeCombiner>()
    private val useCase = UpdateTransactionUseCase(repository, dateAndTimeCombiner)

    private val oldTransaction = Transaction.Empty.copy(
        transactionId = TransactionId("tx-1"),
        date = 1_500L,
    )
    private val anyUpdate = TransactionUpdate(
        type = TransactionType.Spend,
        amount = Money(5000L),
        description = "updated",
        accountId = AccountId("acc-1"),
        categoryId = null,
        date = 2_000L,
    )

    @Test
    fun `update should call repository with combined date and original transactionId`() = runTest {
        every { dateAndTimeCombiner.combineKeepingTimeOf(2_000L, 1_500L) } returns 7_777L
        coEvery { repository.update(any(), any()) } just Runs

        useCase(oldTransaction, anyUpdate)

        coVerify(exactly = 1) {
            repository.update(
                transactionId = TransactionId("tx-1"),
                transactionUpdate = anyUpdate.copy(date = 7_777L),
            )
        }
    }

    @Test
    fun `update should take the time of day from the stored transaction, not from the update`() = runTest {
        every { dateAndTimeCombiner.combineKeepingTimeOf(any(), any()) } returns 0L
        coEvery { repository.update(any(), any()) } just Runs

        useCase(oldTransaction, anyUpdate)

        verify(exactly = 1) {
            dateAndTimeCombiner.combineKeepingTimeOf(
                dateInMillis = anyUpdate.date,
                timeSourceInMillis = oldTransaction.date,
            )
        }
    }

    @Test
    fun `update should keep other update fields intact`() = runTest {
        every { dateAndTimeCombiner.combineKeepingTimeOf(any(), any()) } returns 0L
        coEvery { repository.update(any(), any()) } just Runs

        useCase(oldTransaction, anyUpdate)

        coVerify {
            repository.update(
                transactionId = any(),
                transactionUpdate = match {
                    it.type == anyUpdate.type &&
                        it.amount == anyUpdate.amount &&
                        it.description == anyUpdate.description &&
                        it.accountId == anyUpdate.accountId &&
                        it.categoryId == anyUpdate.categoryId
                },
            )
        }
    }

    @Test
    fun `update should propagate DomainException from repository`() = runTest {
        every { dateAndTimeCombiner.combineKeepingTimeOf(any(), any()) } returns 0L
        coEvery { repository.update(any(), any()) } throws DomainException.NotFound("transaction")

        assertFailsWith<DomainException.NotFound> { useCase(oldTransaction, anyUpdate) }
    }

    @Test
    fun `update should throw ValidationError when amount is zero`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(oldTransaction, anyUpdate.copy(amount = Money(0L)))
        }
        coVerify(exactly = 0) { repository.update(any(), any()) }
    }

    @Test
    fun `update should throw ValidationError when amount is negative`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(oldTransaction, anyUpdate.copy(amount = Money(-100L)))
        }
        coVerify(exactly = 0) { repository.update(any(), any()) }
    }

    @Test
    fun `update should reject moving a transaction into the future and persist nothing`() = runTest {
        val lima = TimeZone.of("America/Lima")
        val today = LocalDate(2026, Month.AUGUST, 11)
        val clock = object : Clock {
            override fun now(): Instant = LocalDateTime(today, LocalTime(9, 0)).toInstant(lima)
        }
        val tomorrow = LocalDate(2026, Month.AUGUST, 12).atStartOfDayIn(lima).toEpochMilliseconds()
        val guarded = UpdateTransactionUseCase(repository, dateAndTimeCombiner, clock, lima)

        every { dateAndTimeCombiner.combineKeepingTimeOf(any(), any()) } returns tomorrow

        val ex = assertFailsWith<DomainException.ValidationError> {
            guarded(oldTransaction, anyUpdate)
        }

        assertEquals(ValidationCode.DateInTheFuture, ex.code)
        coVerify(exactly = 0) { repository.update(any(), any()) }
    }
}
