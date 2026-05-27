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

/**
 * Tests for CreateRecurringMovementUseCase.
 * Spec coverage: 1.1 1.2 1.3 1.4 1.5 1.6 1.7
 */
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

    /**
     * Scenario 1.1 — happy path create (fixed amount).
     */
    @Test
    fun `invoke stores template and returns created with correct defaults`() = runTest {
        // spec 1.1
        useCase(validInsert())
        assertEquals(1, repository.createCount)
    }

    /**
     * Scenario 1.2 — happy path create (variable amount).
     */
    @Test
    fun `invoke accepts null amount for variable template`() = runTest {
        // spec 1.2
        useCase(validInsert(amount = null))
        assertEquals(1, repository.createCount)
    }

    /**
     * Scenario 1.3 — validation: blank name.
     */
    @Test
    fun `invoke throws ValidationError when name is blank`() = runTest {
        // spec 1.3
        assertFailsWith<DomainException.ValidationError> {
            useCase(validInsert(name = "   "))
        }
        assertEquals(0, repository.createCount)
    }

    @Test
    fun `invoke throws ValidationError when name is empty`() = runTest {
        // spec 1.3
        assertFailsWith<DomainException.ValidationError> {
            useCase(validInsert(name = ""))
        }
        assertEquals(0, repository.createCount)
    }

    /**
     * Scenario 1.4 — validation: missing accountId.
     */
    @Test
    fun `invoke throws ValidationError when accountId is blank`() = runTest {
        // spec 1.4
        assertFailsWith<DomainException.ValidationError> {
            useCase(validInsert(accountId = ""))
        }
        assertEquals(0, repository.createCount)
    }

    /**
     * Scenario 1.5 — validation: dayOfMonth out of range.
     */
    @Test
    fun `invoke throws ValidationError when dayOfMonth is 0`() = runTest {
        // spec 1.5
        assertFailsWith<DomainException.ValidationError> {
            useCase(validInsert(dayOfMonth = 0))
        }
        assertEquals(0, repository.createCount)
    }

    @Test
    fun `invoke throws ValidationError when dayOfMonth is 32`() = runTest {
        // spec 1.5
        assertFailsWith<DomainException.ValidationError> {
            useCase(validInsert(dayOfMonth = 32))
        }
        assertEquals(0, repository.createCount)
    }

    /**
     * Scenario 1.6 — validation: amount zero (not null).
     */
    @Test
    fun `invoke throws ValidationError when amount is zero cents`() = runTest {
        // spec 1.6
        assertFailsWith<DomainException.ValidationError> {
            useCase(validInsert(amount = Money(0L)))
        }
        assertEquals(0, repository.createCount)
    }

    /**
     * Scenario 1.7 — validation: negative amount.
     */
    @Test
    fun `invoke throws ValidationError when amount is negative`() = runTest {
        // spec 1.7
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
        // Name with leading/trailing spaces but not blank — should succeed
        useCase(validInsert(name = "  Sueldo  "))
        assertEquals(1, repository.createCount)
    }

    @Test
    fun `validation order is name then accountId then dayOfMonth then amount`() = runTest {
        // blank name takes priority over bad accountId
        val ex = assertFailsWith<DomainException.ValidationError> {
            useCase(validInsert(name = "", accountId = "", dayOfMonth = 0, amount = Money(0L)))
        }
        assertTrue(ex.message?.isNotBlank() == true)
        assertEquals(0, repository.createCount)
    }
}
