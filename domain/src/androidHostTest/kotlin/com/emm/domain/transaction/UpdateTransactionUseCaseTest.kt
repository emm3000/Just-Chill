package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
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

class UpdateTransactionUseCaseTest {

    private val repository = mockk<TransactionRepository>()

    private val lima = TimeZone.of("America/Lima")
    private val today = LocalDate(2026, Month.AUGUST, 11)
    private val clock = object : Clock {
        override fun now(): Instant = LocalDateTime(today, LocalTime(9, 0)).toInstant(lima)
    }

    private val useCase = UpdateTransactionUseCase(repository, clock, lima)

    private val storedOccurredAt = LocalDateTime(2026, Month.MARCH, 3, 21, 47, 33)

    private val oldTransaction = Transaction.Empty.copy(
        transactionId = TransactionId("tx-1"),
        occurredAt = storedOccurredAt,
    )
    private val anyUpdate = TransactionUpdate(
        type = TransactionType.Spend,
        amount = Money(5000L),
        description = "updated",
        accountId = AccountId("acc-1"),
        categoryId = null,
        occurredAt = storedOccurredAt,
    )

    @Test
    fun `update writes occurredAt verbatim against the original transactionId`() = runTest {
        coEvery { repository.update(any(), any()) } just Runs

        val movedToAnotherDay = anyUpdate.copy(
            occurredAt = LocalDateTime(LocalDate(2026, Month.MARCH, 5), storedOccurredAt.time),
        )
        useCase(oldTransaction, movedToAnotherDay)

        coVerify(exactly = 1) {
            repository.update(
                transactionId = TransactionId("tx-1"),
                transactionUpdate = movedToAnotherDay,
            )
        }
    }

    @Test
    fun `an edit that does not touch the date writes back the stored value unchanged`() = runTest {
        coEvery { repository.update(any(), any()) } just Runs

        useCase(oldTransaction, anyUpdate.copy(amount = Money(999L)))

        coVerify {
            repository.update(
                transactionId = any(),
                transactionUpdate = match { it.occurredAt == oldTransaction.occurredAt },
            )
        }
    }

    @Test
    fun `update should keep other update fields intact`() = runTest {
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
        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(oldTransaction, anyUpdate.copy(occurredAt = LocalDateTime(2026, Month.AUGUST, 12, 0, 0)))
        }

        assertEquals(ValidationCode.DateInTheFuture, ex.code)
        coVerify(exactly = 0) { repository.update(any(), any()) }
    }

    @Test
    fun `update accepts a transaction stamped later today than the clock reads`() = runTest {
        coEvery { repository.update(any(), any()) } just Runs

        useCase(oldTransaction, anyUpdate.copy(occurredAt = LocalDateTime(today, LocalTime(23, 0))))

        coVerify(exactly = 1) { repository.update(any(), any()) }
    }
}
