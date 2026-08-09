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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
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

    private val utc = TimeZone.UTC

    @Before
    fun setUp() {
        repository = FakeRecurringMovementRepository()
        useCase = GetPendingRecurringMovementsUseCase(repository, utc)
    }

    private fun epochMillis(year: Int, month: Int, day: Int): Long =
        LocalDate(year, month, day).atStartOfDayIn(utc).toEpochMilliseconds()

    private fun buildTemplate(
        id: String = "rm-1",
        dayOfMonth: Int,
        isActive: Boolean = true,
        lastConfirmedPeriod: String? = null,
        createdAt: Long = epochMillis(2026, 5, 1),
        name: String = "Test $id",
    ) = RecurringMovement(
        id = RecurringMovementId(id),
        name = name,
        type = TransactionType.Spend,
        amount = Money(100_00L),
        description = "",
        categoryId = null,
        accountId = AccountId("acc-1"),
        frequency = Frequency.Monthly,
        dayOfMonth = dayOfMonth,
        isActive = isActive,
        lastConfirmedPeriod = lastConfirmedPeriod,
        createdAt = createdAt,
    )

    /**
     * Scenario 3.1 — inactive template never pending.
     */
    @Test
    fun `returns empty list when template is inactive`() = runTest {
        // spec 3.1
        repository.addTemplate(buildTemplate(dayOfMonth = 1, isActive = false))

        val result = useCase(today = LocalDate(2026, 5, 20)).first()

        assertTrue(result.isEmpty())
    }

    /**
     * Scenario 3.2 — already settled through this period.
     */
    @Test
    fun `returns empty list when the template is settled through this period`() = runTest {
        // spec 3.2
        repository.addTemplate(buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = "2026-05"))

        val result = useCase(today = LocalDate(2026, 5, 20)).first()

        assertTrue(result.isEmpty())
    }

    /**
     * Scenario 3.3 — settled through last period, new month → pending.
     */
    @Test
    fun `returns the template when it was settled in the previous period`() = runTest {
        // spec 3.3
        repository.addTemplate(buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = "2026-04"))

        val result = useCase(today = LocalDate(2026, 5, 1)).first()

        assertEquals(1, result.size)
        assertEquals(YearMonth(2026, Month.MAY), result.first().period)
    }

    /**
     * Scenario 3.4 — never confirmed, before due day → not pending.
     */
    @Test
    fun `returns empty list when never confirmed and before the due day`() = runTest {
        // spec 3.4
        repository.addTemplate(buildTemplate(dayOfMonth = 20, lastConfirmedPeriod = null))

        val result = useCase(today = LocalDate(2026, 5, 10)).first()

        assertTrue(result.isEmpty())
    }

    /**
     * Scenario 3.5 — never confirmed, on due day → pending.
     */
    @Test
    fun `returns the template when never confirmed and on the due day`() = runTest {
        // spec 3.5
        repository.addTemplate(buildTemplate(dayOfMonth = 20, lastConfirmedPeriod = null))

        val result = useCase(today = LocalDate(2026, 5, 20)).first()

        assertEquals(1, result.size)
    }

    /**
     * Scenario 7.1, rewritten. This used to assert exactly ONE item for May with April "silently
     * skipped" — that assertion WAS the bug: April's movement could never be recorded once the
     * calendar moved on. Both months are now returned, oldest first.
     */
    @Test
    fun `returns one item per missed month, oldest first`() = runTest {
        repository.addTemplate(
            buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = "2026-03", createdAt = epochMillis(2026, 1, 1)),
        )

        val result = useCase(today = LocalDate(2026, 5, 15)).first()

        assertEquals(
            listOf(YearMonth(2026, Month.APRIL), YearMonth(2026, Month.MAY)),
            result.map { it.period },
        )
    }

    @Test
    fun `returns only active due templates from a mixed list`() = runTest {
        repository.addTemplate(
            buildTemplate(id = "active-due", dayOfMonth = 1, isActive = true, lastConfirmedPeriod = null),
            buildTemplate(id = "inactive", dayOfMonth = 1, isActive = false, lastConfirmedPeriod = null),
            buildTemplate(id = "confirmed", dayOfMonth = 1, isActive = true, lastConfirmedPeriod = "2026-05"),
            buildTemplate(id = "not-due-yet", dayOfMonth = 25, isActive = true, lastConfirmedPeriod = null),
        )

        val result = useCase(today = LocalDate(2026, 5, 20)).first()

        assertEquals(1, result.size)
        assertEquals("active-due", result.first().movement.id.value)
    }

    @Test
    fun `orders across templates by period, then by name`() = runTest {
        val jan = epochMillis(2026, 1, 1)
        repository.addTemplate(
            buildTemplate(id = "b", name = "Beta", dayOfMonth = 1, lastConfirmedPeriod = "2026-04", createdAt = jan),
            buildTemplate(id = "a", name = "Alfa", dayOfMonth = 1, lastConfirmedPeriod = "2026-04", createdAt = jan),
            buildTemplate(id = "o", name = "Vieja", dayOfMonth = 1, lastConfirmedPeriod = "2026-03", createdAt = jan),
        )

        val result = useCase(today = LocalDate(2026, 5, 15)).first()

        // The catch-up month comes first, then this month's three alphabetically: a queue the user
        // works top to bottom, which is exactly the order the monotonic guard requires.
        assertEquals(
            listOf(
                "Vieja" to Month.APRIL,
                "Alfa" to Month.MAY,
                "Beta" to Month.MAY,
                "Vieja" to Month.MAY,
            ),
            result.map { it.movement.name to it.period.month },
        )
    }
}
