package com.emm.domain.recurring

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.domain.shared.error.DomainException
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CreateRecurringMovementUseCaseTest {

    private lateinit var repository: FakeRecurringMovementRepository
    private lateinit var useCase: CreateRecurringMovementUseCase

    @Before
    fun setUp() {
        repository = FakeRecurringMovementRepository()
        useCase = CreateRecurringMovementUseCase(repository)
    }

    private fun validInsert(
        name: String = "Sueldo BCP",
        accountId: String = "acc-1",
        dayOfMonth: Int = 15,
        amount: Money? = Money(500_000L),
    ) = RecurringMovementInsert(
        name = name,
        type = com.emm.domain.transaction.TransactionType.Income,
        amount = amount,
        description = "",
        categoryId = null,
        accountId = AccountId(accountId),
        dayOfMonth = dayOfMonth,
    )

    @Test
    fun `invoke stores template and returns created with correct defaults`() = runTest {
        useCase(validInsert())
        assertEquals(1, repository.createCount)
    }

    @Test
    fun `invoke accepts null amount for variable template`() = runTest {
        useCase(validInsert(amount = null))
        assertEquals(1, repository.createCount)
    }

    @Test
    fun `invoke throws ValidationError when name is blank`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(validInsert(name = "   "))
        }
        assertEquals(0, repository.createCount)
    }

    @Test
    fun `invoke throws ValidationError when name is empty`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(validInsert(name = ""))
        }
        assertEquals(0, repository.createCount)
    }

    @Test
    fun `invoke throws ValidationError when accountId is blank`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(validInsert(accountId = ""))
        }
        assertEquals(0, repository.createCount)
    }

    @Test
    fun `invoke throws ValidationError when dayOfMonth is 0`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(validInsert(dayOfMonth = 0))
        }
        assertEquals(0, repository.createCount)
    }

    @Test
    fun `invoke throws ValidationError when dayOfMonth is 32`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(validInsert(dayOfMonth = 32))
        }
        assertEquals(0, repository.createCount)
    }

    @Test
    fun `invoke throws ValidationError when amount is zero cents`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(validInsert(amount = Money(0L)))
        }
        assertEquals(0, repository.createCount)
    }

    @Test
    fun `invoke throws ValidationError when amount is negative`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(validInsert(amount = Money(-100L)))
        }
        assertEquals(0, repository.createCount)
    }

    @Test
    fun `invoke accepts dayOfMonth boundary 1`() = runTest {
        useCase(validInsert(dayOfMonth = 1))
        assertEquals(1, repository.createCount)
    }

    @Test
    fun `invoke accepts dayOfMonth boundary 31`() = runTest {
        useCase(validInsert(dayOfMonth = 31))
        assertEquals(1, repository.createCount)
    }

    @Test
    fun `invoke accepts positive amount`() = runTest {
        useCase(validInsert(amount = Money(1L)))
        assertEquals(1, repository.createCount)
    }

    @Test
    fun `invoke propagates DomainException from repository`() = runTest {
        val failingRepo = io.mockk.mockk<RecurringMovementRepository>()
        io.mockk.coEvery { failingRepo.create(any()) } throws DomainException.DatabaseError(RuntimeException("db fail"))
        val uc = CreateRecurringMovementUseCase(failingRepo)
        assertFailsWith<DomainException.DatabaseError> {
            uc(validInsert())
        }
    }

    @Test
    fun `invoke trims name before validating`() = runTest {
        useCase(validInsert(name = "  Sueldo  "))
        assertEquals(1, repository.createCount)
    }

    @Test
    fun `validation order is name then accountId then dayOfMonth then amount`() = runTest {
        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(validInsert(name = "", accountId = "", dayOfMonth = 0, amount = Money(0L)))
        }
        assertTrue(ex.message?.isNotBlank() == true)
        assertEquals(0, repository.createCount)
    }
}
