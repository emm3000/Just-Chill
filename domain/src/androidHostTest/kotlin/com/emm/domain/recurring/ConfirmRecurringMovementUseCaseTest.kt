package com.emm.domain.recurring

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.YearMonth
import com.emm.domain.shared.error.DomainException
import com.emm.domain.transaction.TransactionType
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Tests for ConfirmRecurringMovementUseCase.
 * Spec coverage: 4.1 4.2 5.2 5.3 6.1
 *
 * A fixed UTC timezone is injected so date-millis assertions are deterministic
 * regardless of the machine's local timezone.
 */
class ConfirmRecurringMovementUseCaseTest {

    private lateinit var repository: FakeRecurringMovementRepository
    private lateinit var useCase: ConfirmRecurringMovementUseCase

    private val utc = TimeZone.UTC
    private val may2026 = YearMonth(2026, Month.MAY)

    // The fixed template is due on the 15th, so confirming May books it on 2026-05-15, NOT on the
    // day the user happened to tap confirm.
    private val expectedOccurredAt = LocalDateTime(2026, 5, 15, 0, 0)

    private val createdAt = LocalDate(2026, 1, 1).atStartOfDayIn(utc).toEpochMilliseconds()

    private val fixedTemplate = RecurringMovement(
        id = RecurringMovementId("rm-fixed"),
        name = "Netflix",
        type = TransactionType.Spend,
        amount = Money(180_000L),
        description = "Netflix mensual",
        categoryId = CategoryId("cat-1"),
        accountId = AccountId("acc-1"),
        frequency = Frequency.Monthly,
        dayOfMonth = 15,
        isActive = true,
        lastConfirmedPeriod = null,
        createdAt = createdAt,
    )

    private val variableTemplate = RecurringMovement(
        id = RecurringMovementId("rm-variable"),
        name = "Sueldo",
        type = TransactionType.Income,
        amount = null,
        description = "Sueldo mensual",
        categoryId = null,
        accountId = AccountId("acc-2"),
        frequency = Frequency.Monthly,
        dayOfMonth = 1,
        isActive = true,
        lastConfirmedPeriod = null,
        createdAt = createdAt,
    )

    @Before
    fun setUp() {
        repository = FakeRecurringMovementRepository()
        repository.addTemplate(fixedTemplate, variableTemplate)
        useCase = ConfirmRecurringMovementUseCase(repository)
    }

    /**
     * Scenario 4.1 — confirm fixed amount successfully.
     */
    @Test
    fun `invoke fixed amount calls repo confirm exactly once with correct TransactionInsert`() = runTest {
        // spec 4.1
        useCase(
            templateId = RecurringMovementId("rm-fixed"),
            yearMonth = may2026,
            callerAmount = null,
        )
        assertEquals(1, repository.confirmCount)
        val insert = repository.lastConfirmInsert
        assertNotNull(insert)
        assertEquals(Money(180_000L), insert.amount)
        assertEquals(AccountId("acc-1"), insert.accountId)
        assertEquals(CategoryId("cat-1"), insert.categoryId)
        assertEquals(TransactionType.Spend, insert.type)
        assertEquals("2026-05", repository.lastConfirmPeriod)
    }

    /**
     * Scenario 4.2 — confirm fails: repo throws DomainException.
     */
    @Test
    fun `invoke propagates DomainException from repo confirm`() = runTest {
        // spec 4.2
        val failingRepo = io.mockk.mockk<RecurringMovementRepository>()
        io.mockk.coEvery { failingRepo.find(RecurringMovementId("rm-fixed")) } returns fixedTemplate
        io.mockk.coEvery {
            failingRepo.confirm(any(), any(), any())
        } throws DomainException.DatabaseError(RuntimeException("atomic fail"))
        val uc = ConfirmRecurringMovementUseCase(failingRepo)
        assertFailsWith<DomainException.DatabaseError> {
            uc(
                templateId = RecurringMovementId("rm-fixed"),
                yearMonth = may2026,
                callerAmount = null,
            )
        }
    }

    /**
     * Scenario 5.2 — variable amount: confirm with valid user-supplied amount.
     */
    @Test
    fun `invoke variable template uses callerAmount when template amount is null`() = runTest {
        // spec 5.2
        useCase(
            templateId = RecurringMovementId("rm-variable"),
            yearMonth = may2026,
            callerAmount = Money(350_000L),
        )
        assertEquals(1, repository.confirmCount)
        assertEquals(Money(350_000L), repository.lastConfirmInsert?.amount)
    }

    /**
     * Scenario 5.3 — variable amount: zero supplied by caller → ValidationError.
     */
    @Test
    fun `invoke throws ValidationError when variable template and callerAmount is zero`() = runTest {
        // spec 5.3
        assertFailsWith<DomainException.ValidationError> {
            useCase(
                templateId = RecurringMovementId("rm-variable"),
                yearMonth = may2026,
                callerAmount = Money(0L),
            )
        }
        assertEquals(0, repository.confirmCount)
    }

    @Test
    fun `invoke throws ValidationError when variable template and callerAmount is null`() = runTest {
        // spec 5.3 variant: null callerAmount for variable template
        assertFailsWith<DomainException.ValidationError> {
            useCase(
                templateId = RecurringMovementId("rm-variable"),
                yearMonth = may2026,
                callerAmount = null,
            )
        }
        assertEquals(0, repository.confirmCount)
    }

    @Test
    fun `invoke throws ValidationError when variable template and callerAmount is negative`() = runTest {
        assertFailsWith<DomainException.ValidationError> {
            useCase(
                templateId = RecurringMovementId("rm-variable"),
                yearMonth = may2026,
                callerAmount = Money(-1L),
            )
        }
        assertEquals(0, repository.confirmCount)
    }

    /**
     * Scenario 6.1 — idempotency: double confirm same period rejected.
     */
    @Test
    fun `invoke throws ValidationError when template already confirmed this period`() = runTest {
        // spec 6.1
        // First confirm — succeeds
        useCase(
            templateId = RecurringMovementId("rm-fixed"),
            yearMonth = may2026,
            callerAmount = null,
        )
        assertEquals(1, repository.confirmCount)

        // Second confirm same period — should fail with ValidationError
        assertFailsWith<DomainException.ValidationError> {
            useCase(
                templateId = RecurringMovementId("rm-fixed"),
                yearMonth = may2026,
                callerAmount = null,
            )
        }
        // repo.confirm NOT called a second time
        assertEquals(1, repository.confirmCount)
    }

    @Test
    fun `invoke throws NotFound when template does not exist`() = runTest {
        assertFailsWith<DomainException.NotFound> {
            useCase(
                templateId = RecurringMovementId("ghost"),
                yearMonth = may2026,
                callerAmount = null,
            )
        }
        assertEquals(0, repository.confirmCount)
    }

    /**
     * The transaction lands at midnight on the period's own due day — a calendar fact that needs
     * no clock and no timezone to state.
     */
    @Test
    fun `invoke dates the transaction on the period's due day`() = runTest {
        useCase(
            templateId = RecurringMovementId("rm-fixed"),
            yearMonth = may2026,
            callerAmount = null,
        )
        val insert = repository.lastConfirmInsert
        assertNotNull(insert)
        assertEquals(expectedOccurredAt, insert.occurredAt)
    }

    // ── catch-up ──────────────────────────────────────────────────────────────

    @Test
    fun `a caught-up period is booked in its own month, not in the current one`() = runTest {
        // Settling March while it is May has to date the transaction in March, or Home and Reporte
        // disagree about the month the money moved.
        useCase(
            templateId = RecurringMovementId("rm-fixed"),
            yearMonth = YearMonth(2026, Month.MARCH),
            callerAmount = null,
        )

        assertEquals(LocalDateTime(2026, 3, 15, 0, 0), repository.lastConfirmInsert?.occurredAt)
        assertEquals("2026-03", repository.lastConfirmPeriod)
    }

    @Test
    fun `two missed months are settled oldest first`() = runTest {
        useCase(RecurringMovementId("rm-fixed"), YearMonth(2026, Month.MARCH), null)
        useCase(RecurringMovementId("rm-fixed"), YearMonth(2026, Month.APRIL), null)

        assertEquals(2, repository.confirmCount)
        assertEquals("2026-04", repository.lastConfirmPeriod)
    }

    @Test
    fun `confirming a period at or before the mark is rejected`() = runTest {
        useCase(RecurringMovementId("rm-fixed"), YearMonth(2026, Month.APRIL), null)

        // March is now behind the mark. Accepting it would rewind the mark and resurrect April.
        assertFailsWith<DomainException.ValidationError> {
            useCase(RecurringMovementId("rm-fixed"), YearMonth(2026, Month.MARCH), null)
        }
        assertEquals(1, repository.confirmCount)
    }

    @Test
    fun `the due day is clamped to short months when catching up`() = runTest {
        val endOfMonth = fixedTemplate.copy(id = RecurringMovementId("rm-eom"), dayOfMonth = 31)
        repository.addTemplate(endOfMonth)

        useCase(RecurringMovementId("rm-eom"), YearMonth(2026, Month.FEBRUARY), null)

        assertEquals(LocalDateTime(2026, 2, 28, 0, 0), repository.lastConfirmInsert?.occurredAt)
    }

    @Test
    fun `invoke description from template is copied to transaction`() = runTest {
        useCase(
            templateId = RecurringMovementId("rm-fixed"),
            yearMonth = may2026,
            callerAmount = null,
        )
        assertEquals("Netflix mensual", repository.lastConfirmInsert?.description)
    }
}
