package com.emm.domain.recurring

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.error.DomainException
import com.emm.domain.transaction.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests for UpdateRecurringMovementUseCase.
 * Spec coverage: 8.1 8.2 8.3
 */
class UpdateRecurringMovementUseCaseTest {

    private lateinit var repository: FakeRecurringMovementRepository
    private lateinit var useCase: UpdateRecurringMovementUseCase

    private val existingTemplate = RecurringMovement(
        id = RecurringMovementId("rm-1"),
        name = "Netflix",
        type = TransactionType.Spend,
        amount = Money(100_000L),
        description = "",
        categoryId = null,
        accountId = AccountId("acc-1"),
        frequency = Frequency.Monthly,
        dayOfMonth = 15,
        isActive = true,
        lastConfirmedPeriod = null,
    )

    @Before
    fun setUp() {
        repository = FakeRecurringMovementRepository()
        repository.addTemplate(existingTemplate)
        useCase = UpdateRecurringMovementUseCase(repository)
    }

    private fun validUpdateInsert(
        name: String = "Netflix HD",
        dayOfMonth: Int = 20,
        amount: Money? = Money(100_000L),
        isActive: Boolean = true,
    ) = RecurringMovementInsert(
        name = name,
        type = TransactionType.Spend,
        amount = amount,
        description = "",
        categoryId = null,
        accountId = AccountId("acc-1"),
        dayOfMonth = dayOfMonth,
        isActive = isActive,
    )

    /**
     * Scenario 8.1 — edit name and dayOfMonth.
     */
    @Test
    fun `invoke delegates update to repository and does not touch transactions`() = runTest {
        // spec 8.1
        useCase(RecurringMovementId("rm-1"), validUpdateInsert())
        assertEquals(1, repository.updateCount)
        assertEquals(0, repository.confirmCount)
    }

    /**
     * Scenario 8.2 — set isActive to false.
     */
    @Test
    fun `invoke updates isActive to false`() = runTest {
        // spec 8.2
        useCase(RecurringMovementId("rm-1"), validUpdateInsert(isActive = false))
        assertEquals(1, repository.updateCount)
    }

    /**
     * Scenario 8.3 — change amount from fixed to variable (null).
     */
    @Test
    fun `invoke accepts null amount change from fixed to variable`() = runTest {
        // spec 8.3
        useCase(RecurringMovementId("rm-1"), validUpdateInsert(amount = null))
        assertEquals(1, repository.updateCount)
    }

    @Test
    fun `invoke throws ValidationError when name is blank`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(RecurringMovementId("rm-1"), validUpdateInsert(name = ""))
        }
        assertEquals(0, repository.updateCount)
    }

    @Test
    fun `invoke throws ValidationError when dayOfMonth is out of range`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(RecurringMovementId("rm-1"), validUpdateInsert(dayOfMonth = 0))
        }
        assertEquals(0, repository.updateCount)
    }

    @Test
    fun `invoke throws ValidationError when amount is zero`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(RecurringMovementId("rm-1"), validUpdateInsert(amount = Money(0L)))
        }
        assertEquals(0, repository.updateCount)
    }

    @Test
    fun `invoke throws ValidationError when amount is negative`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(RecurringMovementId("rm-1"), validUpdateInsert(amount = Money(-1L)))
        }
        assertEquals(0, repository.updateCount)
    }
}
