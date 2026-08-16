package com.emm.domain.recurring

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.YearMonth
import com.emm.domain.shared.error.DomainException
import com.emm.domain.transaction.TransactionType
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SkipRecurringMovementUseCaseTest {

    private lateinit var repository: FakeRecurringMovementRepository
    private lateinit var useCase: SkipRecurringMovementUseCase

    private val template = RecurringMovement(
        id = RecurringMovementId("rm-1"),
        name = "Gimnasio",
        type = TransactionType.Spend,
        amount = Money(120_000L),
        description = "",
        categoryId = null,
        accountId = AccountId("acc-1"),
        frequency = Frequency.Monthly,
        dayOfMonth = 5,
        isActive = true,
        lastConfirmedPeriod = "2026-03",
        createdAt = LocalDate(2026, 1, 1).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
    )

    @Before
    fun setUp() {
        repository = FakeRecurringMovementRepository()
        repository.addTemplate(template)
        useCase = SkipRecurringMovementUseCase(repository)
    }

    @Test
    fun `skipping advances the mark without booking a transaction`() = runTest {
        useCase(RecurringMovementId("rm-1"), YearMonth(2026, Month.APRIL))

        assertEquals(1, repository.skipCount)
        assertEquals("2026-04", repository.lastSkipPeriod)
        assertEquals(0, repository.confirmCount)
    }

    @Test
    fun `a skipped month stops being pending`() = runTest {
        useCase(RecurringMovementId("rm-1"), YearMonth(2026, Month.APRIL))

        val updated = repository.find(RecurringMovementId("rm-1"))!!
        val stillPending = pendingPeriods(updated, LocalDate(2026, 5, 20), TimeZone.UTC)

        assertEquals(listOf(YearMonth(2026, Month.MAY)), stillPending)
    }

    @Test
    fun `skipping a period at or before the mark is rejected`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(RecurringMovementId("rm-1"), YearMonth(2026, Month.MARCH))
        }
        assertEquals(0, repository.skipCount)
    }

    @Test
    fun `skipping an unknown template throws NotFound`() = runTest {
        assertFailsWith<DomainException.NotFound> {
            useCase(RecurringMovementId("ghost"), YearMonth(2026, Month.APRIL))
        }
        assertEquals(0, repository.skipCount)
    }
}
