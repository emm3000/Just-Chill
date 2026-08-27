package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
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

class CreateTransactionUseCaseTest {

    private val repository = mockk<TransactionRepository>()
    private val idProvider = mockk<UniqueIdProvider>()

    private val lima = TimeZone.of("America/Lima")
    private val today = LocalDate(2026, Month.AUGUST, 11)
    private val clock = object : Clock {
        override fun now(): Instant = LocalDateTime(today, LocalTime(9, 0)).toInstant(lima)
    }

    private val useCase = CreateTransactionUseCase(repository, idProvider, clock, lima)

    private val occurredAt = LocalDateTime(2026, Month.AUGUST, 10, 21, 47, 33)

    private val anyInsert = TransactionInsert(
        type = TransactionType.Income,
        amount = Money(10000L),
        description = "desc",
        categoryId = null,
        occurredAt = occurredAt,
        accountId = AccountId("acc-1"),
    )

    @Test
    fun `create stores occurredAt exactly as given`() = runTest {
        every { idProvider.id } returns "fixed-id"
        coEvery { repository.create(any()) } just Runs

        useCase(anyInsert)

        coVerify(exactly = 1) {
            repository.create(anyInsert.copy(id = TransactionId("fixed-id")))
        }
    }

    @Test
    fun `create does not touch the time of day it was handed`() = runTest {
        every { idProvider.id } returns "id"
        coEvery { repository.create(any()) } just Runs

        useCase(anyInsert)

        coVerify { repository.create(match { it.occurredAt == occurredAt }) }
    }

    @Test
    fun `create should overwrite caller-provided id with idProvider`() = runTest {
        every { idProvider.id } returns "generated"
        coEvery { repository.create(any()) } just Runs

        useCase(anyInsert.copy(id = TransactionId("caller-tried-this")))

        coVerify {
            repository.create(match { it.id == TransactionId("generated") })
        }
    }

    @Test
    fun `create should propagate DomainException from repository`() = runTest {
        every { idProvider.id } returns "id"
        coEvery { repository.create(any()) } throws DomainException.DatabaseError(RuntimeException("boom"))

        val ex = assertFailsWith<DomainException.DatabaseError> { useCase(anyInsert) }
        assertEquals("boom", ex.message)
    }

    @Test
    fun `create should throw ValidationError when amount is zero`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(anyInsert.copy(amount = Money(0L)))
        }
        coVerify(exactly = 0) { repository.create(any()) }
    }

    @Test
    fun `create should throw ValidationError when amount is negative`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(anyInsert.copy(amount = Money(-100L)))
        }
        coVerify(exactly = 0) { repository.create(any()) }
    }

    @Test
    fun `create should reject a date in the future and persist nothing`() = runTest {
        every { idProvider.id } returns "id"

        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(anyInsert.copy(occurredAt = LocalDateTime(2026, Month.AUGUST, 12, 0, 0)))
        }

        assertEquals(ValidationCode.DateInTheFuture, ex.code)
        coVerify(exactly = 0) { repository.create(any()) }
    }

    @Test
    fun `create accepts a movement stamped later today than the clock reads`() = runTest {
        every { idProvider.id } returns "id"
        coEvery { repository.create(any()) } just Runs

        useCase(anyInsert.copy(occurredAt = LocalDateTime(today, LocalTime(23, 0))))

        coVerify(exactly = 1) { repository.create(any()) }
    }
}
