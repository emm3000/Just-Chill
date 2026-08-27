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

class DeleteRecurringMovementUseCaseTest {

    private lateinit var repository: FakeRecurringMovementRepository
    private lateinit var useCase: DeleteRecurringMovementUseCase

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
        createdAt = 0L,
    )

    @Before
    fun setUp() {
        repository = FakeRecurringMovementRepository()
        repository.addTemplate(existingTemplate)
        useCase = DeleteRecurringMovementUseCase(repository)
    }

    @Test
    fun `invoke calls repository delete and does not touch transaction repository`() = runTest {
        useCase(RecurringMovementId("rm-1"))
        assertEquals(1, repository.deleteCount)
        assertEquals(0, repository.confirmCount)
    }

    @Test
    fun `invoke deleted template can no longer be found`() = runTest {
        useCase(RecurringMovementId("rm-1"))
        val found = repository.find(RecurringMovementId("rm-1"))
        assertEquals(null, found)
    }

    @Test
    fun `invoke throws NotFound when template does not exist`() = runTest {
        assertFailsWith<DomainException.NotFound> {
            useCase(RecurringMovementId("T_GHOST"))
        }
        assertEquals(0, repository.deleteCount)
    }
}
