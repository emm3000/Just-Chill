package com.emm.domain.recurring

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for GetPendingRecurringMovementsUseCase.
 * Spec coverage: 3.1 3.2 3.3 3.4 3.5 7.1
 */
class GetPendingRecurringMovementsUseCaseTest {

    private lateinit var repository: FakeRecurringMovementRepository
    private lateinit var useCase: GetPendingRecurringMovementsUseCase

    @Before
    fun setUp() {
        repository = FakeRecurringMovementRepository()
        useCase = GetPendingRecurringMovementsUseCase(repository)
    }

    private fun buildTemplate(
        id: String = "rm-1",
        dayOfMonth: Int,
        isActive: Boolean = true,
        lastConfirmedPeriod: String? = null,
    ) = RecurringMovement(
        id = RecurringMovementId(id),
        name = "Test $id",
        type = TransactionType.Spend,
        amount = Money(100_00L),
        description = "",
        categoryId = null,
        accountId = AccountId("acc-1"),
        frequency = Frequency.Monthly,
        dayOfMonth = dayOfMonth,
        isActive = isActive,
        lastConfirmedPeriod = lastConfirmedPeriod,
    )

    /**
     * Scenario 3.1 — inactive template never pending.
     */
    @Test
    fun `returns empty list when template is inactive`() = runTest {
        // spec 3.1
        repository.addTemplate(buildTemplate(dayOfMonth = 1, isActive = false))
        val result = useCase(
            today = LocalDate(2026, 5, 20),
            yearMonth = YearMonth(2026, Month.MAY),
        ).first()
        assertTrue(result.isEmpty())
    }

    /**
     * Scenario 3.2 — already confirmed this period.
     */
    @Test
    fun `returns empty list when template already confirmed this period`() = runTest {
        // spec 3.2
        repository.addTemplate(buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = "2026-05"))
        val result = useCase(
            today = LocalDate(2026, 5, 20),
            yearMonth = YearMonth(2026, Month.MAY),
        ).first()
        assertTrue(result.isEmpty())
    }

    /**
     * Scenario 3.3 — confirmed last period, new month → pending.
     */
    @Test
    fun `returns template when confirmed in previous period`() = runTest {
        // spec 3.3
        repository.addTemplate(buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = "2026-04"))
        val result = useCase(
            today = LocalDate(2026, 5, 1),
            yearMonth = YearMonth(2026, Month.MAY),
        ).first()
        assertEquals(1, result.size)
    }

    /**
     * Scenario 3.4 — never confirmed, before due day → not pending.
     */
    @Test
    fun `returns empty list when never confirmed and before due day`() = runTest {
        // spec 3.4
        repository.addTemplate(buildTemplate(dayOfMonth = 20, lastConfirmedPeriod = null))
        val result = useCase(
            today = LocalDate(2026, 5, 10),
            yearMonth = YearMonth(2026, Month.MAY),
        ).first()
        assertTrue(result.isEmpty())
    }

    /**
     * Scenario 3.5 — never confirmed, on due day → pending.
     */
    @Test
    fun `returns template when never confirmed and on due day`() = runTest {
        // spec 3.5
        repository.addTemplate(buildTemplate(dayOfMonth = 20, lastConfirmedPeriod = null))
        val result = useCase(
            today = LocalDate(2026, 5, 20),
            yearMonth = YearMonth(2026, Month.MAY),
        ).first()
        assertEquals(1, result.size)
    }

    /**
     * Scenario 7.1 — no retroactive catch-up: skipped April, today is May 15.
     * GetPendingRecurringMovementsUseCase is invoked for May ONLY → exactly ONE result.
     * April is silently skipped.
     */
    @Test
    fun `returns exactly one pending item for current month even if previous month was skipped`() = runTest {
        // spec 7.1: lastConfirmedPeriod=2026-03, today=May 15
        repository.addTemplate(buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = "2026-03"))
        val result = useCase(
            today = LocalDate(2026, 5, 15),
            yearMonth = YearMonth(2026, Month.MAY),
        ).first()
        // Exactly ONE item for May — no April catch-up item
        assertEquals(1, result.size)
    }

    @Test
    fun `returns only active pending templates from a mixed list`() = runTest {
        repository.addTemplate(
            buildTemplate(id = "active-due", dayOfMonth = 1, isActive = true, lastConfirmedPeriod = null),
            buildTemplate(id = "inactive", dayOfMonth = 1, isActive = false, lastConfirmedPeriod = null),
            buildTemplate(id = "confirmed", dayOfMonth = 1, isActive = true, lastConfirmedPeriod = "2026-05"),
            buildTemplate(id = "not-due-yet", dayOfMonth = 25, isActive = true, lastConfirmedPeriod = null),
        )
        val result = useCase(
            today = LocalDate(2026, 5, 20),
            yearMonth = YearMonth(2026, Month.MAY),
        ).first()
        assertEquals(1, result.size)
        assertEquals("active-due", result.first().id.value)
    }
}
