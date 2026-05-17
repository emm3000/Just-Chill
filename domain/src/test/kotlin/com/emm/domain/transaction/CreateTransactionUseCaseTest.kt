package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.DateAndTimeCombiner
import com.emm.domain.shared.TransactionId
import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.shared.error.DomainException
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreateTransactionUseCaseTest {

    private val repository = mockk<TransactionRepository>()
    private val dateAndTimeCombiner = mockk<DateAndTimeCombiner>()
    private val idProvider = mockk<UniqueIdProvider>()
    private val useCase = CreateTransactionUseCase(repository, dateAndTimeCombiner, idProvider)

    private val anyInsert = TransactionInsert(
        type = TransactionType.Income,
        amount = 100.0,
        description = "desc",
        categoryId = null,
        date = 1_000L,
        accountId = AccountId("acc-1"),
    )

    @Test
    fun `create should call repository with combined date and provided id`() = runTest {
        every { idProvider.id } returns "fixed-id"
        every { dateAndTimeCombiner.combineWithUtc(1_000L) } returns 9_999L
        coEvery { repository.create(any()) } just Runs

        useCase(anyInsert)

        coVerify(exactly = 1) {
            repository.create(
                anyInsert.copy(id = TransactionId("fixed-id"), date = 9_999L),
            )
        }
    }

    @Test
    fun `create should overwrite caller-provided id with idProvider`() = runTest {
        every { idProvider.id } returns "generated"
        every { dateAndTimeCombiner.combineWithUtc(any()) } returns 0L
        coEvery { repository.create(any()) } just Runs

        useCase(anyInsert.copy(id = TransactionId("caller-tried-this")))

        coVerify {
            repository.create(match { it.id == TransactionId("generated") })
        }
    }

    @Test
    fun `create should propagate DomainException from repository`() = runTest {
        every { idProvider.id } returns "id"
        every { dateAndTimeCombiner.combineWithUtc(any()) } returns 0L
        coEvery { repository.create(any()) } throws DomainException.DatabaseError(RuntimeException("boom"))

        val ex = assertFailsWith<DomainException.DatabaseError> { useCase(anyInsert) }
        assertEquals("boom", ex.message)
    }

    @Test
    fun `create should throw ValidationError when amount is zero`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(anyInsert.copy(amount = 0.0))
        }
        coVerify(exactly = 0) { repository.create(any()) }
    }

    @Test
    fun `create should throw ValidationError when amount is negative`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(anyInsert.copy(amount = -1.0))
        }
        coVerify(exactly = 0) { repository.create(any()) }
    }
}
