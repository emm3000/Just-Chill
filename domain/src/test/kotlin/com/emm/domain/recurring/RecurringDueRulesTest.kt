package com.emm.domain.recurring

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for RecurringDueRules.kt
 * Spec coverage: 2.1 2.2 2.3 2.4 2.5 2.6 3.1 3.2 3.3 3.4 3.5 7.1
 */
class RecurringDueRulesTest {

    // ── lengthOfMonth helpers ──────────────────────────────────────────────────

    @Test
    fun `lengthOfMonth returns 28 for Feb in non-leap year`() {
        assertEquals(28, lengthOfMonth(YearMonth(2026, Month.FEBRUARY)))
    }

    @Test
    fun `lengthOfMonth returns 29 for Feb in leap year`() {
        assertEquals(29, lengthOfMonth(YearMonth(2024, Month.FEBRUARY)))
    }

    @Test
    fun `lengthOfMonth returns 30 for April`() {
        assertEquals(30, lengthOfMonth(YearMonth(2026, Month.APRIL)))
    }

    @Test
    fun `lengthOfMonth returns 31 for January`() {
        assertEquals(31, lengthOfMonth(YearMonth(2026, Month.JANUARY)))
    }

    // ── effectiveDueDay clamping ──────────────────────────────────────────────

    /**
     * Scenario 2.1 — day 15 in Feb, no clamping needed.
     */
    @Test
    fun `effectiveDueDay does not clamp day 15 in non-leap Feb`() {
        // spec 2.1
        val result = effectiveDueDay(15, YearMonth(2026, Month.FEBRUARY))
        assertEquals(15, result)
    }

    /**
     * Scenario 2.2 — day 31 clamped to 28 in Feb non-leap.
     */
    @Test
    fun `effectiveDueDay clamps day 31 to 28 in non-leap February`() {
        // spec 2.2
        val result = effectiveDueDay(31, YearMonth(2026, Month.FEBRUARY))
        assertEquals(28, result)
    }

    /**
     * Scenario 2.3 — day 31 clamped to 29 in Feb leap year.
     */
    @Test
    fun `effectiveDueDay clamps day 31 to 29 in leap February`() {
        // spec 2.3
        val result = effectiveDueDay(31, YearMonth(2024, Month.FEBRUARY))
        assertEquals(29, result)
    }

    /**
     * Scenario 2.4 — day 31 clamped to 30 in April (30-day month).
     */
    @Test
    fun `effectiveDueDay clamps day 31 to 30 in April`() {
        // spec 2.4
        val result = effectiveDueDay(31, YearMonth(2026, Month.APRIL))
        assertEquals(30, result)
    }

    /**
     * Scenario 2.5 — day 1 is never clamped.
     */
    @Test
    fun `effectiveDueDay returns 1 for day 1 any month`() {
        // spec 2.5
        assertEquals(1, effectiveDueDay(1, YearMonth(2026, Month.FEBRUARY)))
        assertEquals(1, effectiveDueDay(1, YearMonth(2026, Month.MARCH)))
    }

    // ── periodKey format ──────────────────────────────────────────────────────

    @Test
    fun `periodKey formats correctly as YYYY-MM`() {
        assertEquals("2026-02", periodKey(YearMonth(2026, Month.FEBRUARY)))
        assertEquals("2026-12", periodKey(YearMonth(2026, Month.DECEMBER)))
        assertEquals("2026-01", periodKey(YearMonth(2026, Month.JANUARY)))
    }

    // ── isPending boundaries ──────────────────────────────────────────────────

    /**
     * Scenario 2.6 — before due day → not pending.
     */
    @Test
    fun `isPending returns false when today is before due day`() {
        // spec 2.6: dayOfMonth=15, today=Feb 5
        val rm = buildTemplate(dayOfMonth = 15)
        val result = isPending(rm, YearMonth(2026, Month.FEBRUARY), LocalDate(2026, 2, 5))
        assertFalse(result)
    }

    /**
     * Scenario 2.1 — on or after due day → pending (mid-month, no clamping).
     */
    @Test
    fun `isPending returns true when today equals due day`() {
        // spec 2.1 & 3.5
        val rm = buildTemplate(dayOfMonth = 15)
        val result = isPending(rm, YearMonth(2026, Month.FEBRUARY), LocalDate(2026, 2, 15))
        assertTrue(result)
    }

    @Test
    fun `isPending returns true when today is after due day`() {
        val rm = buildTemplate(dayOfMonth = 15)
        val result = isPending(rm, YearMonth(2026, Month.FEBRUARY), LocalDate(2026, 2, 20))
        assertTrue(result)
    }

    /**
     * Scenario 3.1 — inactive template never pending.
     */
    @Test
    fun `isPending returns false when template is inactive`() {
        // spec 3.1
        val rm = buildTemplate(dayOfMonth = 1, isActive = false)
        val result = isPending(rm, YearMonth(2026, Month.MAY), LocalDate(2026, 5, 20))
        assertFalse(result)
    }

    /**
     * Scenario 3.2 — already confirmed this period → not pending.
     */
    @Test
    fun `isPending returns false when lastConfirmedPeriod equals current period`() {
        // spec 3.2
        val rm = buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = "2026-05")
        val result = isPending(rm, YearMonth(2026, Month.MAY), LocalDate(2026, 5, 20))
        assertFalse(result)
    }

    /**
     * Scenario 3.3 — confirmed last period, new month → pending.
     */
    @Test
    fun `isPending returns true when lastConfirmedPeriod is previous month`() {
        // spec 3.3
        val rm = buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = "2026-04")
        val result = isPending(rm, YearMonth(2026, Month.MAY), LocalDate(2026, 5, 1))
        assertTrue(result)
    }

    /**
     * Scenario 3.4 — never confirmed, before due day → not pending.
     */
    @Test
    fun `isPending returns false when never confirmed and today is before due day`() {
        // spec 3.4
        val rm = buildTemplate(dayOfMonth = 20, lastConfirmedPeriod = null)
        val result = isPending(rm, YearMonth(2026, Month.MAY), LocalDate(2026, 5, 10))
        assertFalse(result)
    }

    /**
     * Scenario 3.5 — never confirmed, on due day → pending.
     */
    @Test
    fun `isPending returns true when never confirmed and today equals due day`() {
        // spec 3.5
        val rm = buildTemplate(dayOfMonth = 20, lastConfirmedPeriod = null)
        val result = isPending(rm, YearMonth(2026, Month.MAY), LocalDate(2026, 5, 20))
        assertTrue(result)
    }

    /**
     * Scenario 2.2 — clamp: day 31 in non-leap Feb → effectiveDay=28; today=28 → pending.
     */
    @Test
    fun `isPending with day 31 template on last day of non-leap February is pending`() {
        // spec 2.2
        val rm = buildTemplate(dayOfMonth = 31)
        val result = isPending(rm, YearMonth(2026, Month.FEBRUARY), LocalDate(2026, 2, 28))
        assertTrue(result)
    }

    /**
     * Scenario 2.3 — clamp: day 31 in leap Feb → effectiveDay=29; today=29 → pending.
     */
    @Test
    fun `isPending with day 31 template on last day of leap February is pending`() {
        // spec 2.3
        val rm = buildTemplate(dayOfMonth = 31)
        val result = isPending(rm, YearMonth(2024, Month.FEBRUARY), LocalDate(2024, 2, 29))
        assertTrue(result)
    }

    /**
     * Scenario 2.4 — clamp: day 31 in April → effectiveDay=30; today=30 → pending.
     */
    @Test
    fun `isPending with day 31 template on April 30 is pending`() {
        // spec 2.4
        val rm = buildTemplate(dayOfMonth = 31)
        val result = isPending(rm, YearMonth(2026, Month.APRIL), LocalDate(2026, 4, 30))
        assertTrue(result)
    }

    /**
     * Scenario 7.1 — skipped month: confirmed in March, today is May 15.
     * The use case is called for "2026-05" only → exactly ONE result.
     * Testing the predicate level: May isPending = true; April (skipped) is never queried.
     */
    @Test
    fun `isPending returns true for current month even after skipped month`() {
        // spec 7.1: lastConfirmedPeriod=2026-03, today=May 15
        val rm = buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = "2026-03")
        // May: not confirmed → pending on day 1
        val mayResult = isPending(rm, YearMonth(2026, Month.MAY), LocalDate(2026, 5, 15))
        assertTrue(mayResult)
        // April was skipped but we never call isPending for April — the use case only uses current period
        // Verify April would also be "pending" (predicate doesn't know about skip, just period mismatch)
        val aprResult = isPending(rm, YearMonth(2026, Month.APRIL), LocalDate(2026, 4, 15))
        assertTrue(aprResult)
        // The important thing is GetPendingRecurringMovementsUseCase only asks for ONE period at a time
        // and returns at most one item per template (tested in GetPendingRecurringMovementsUseCaseTest)
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private fun buildTemplate(dayOfMonth: Int, isActive: Boolean = true, lastConfirmedPeriod: String? = null) =
        RecurringMovement(
            id = RecurringMovementId("rm-1"),
            name = "Test",
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
}
