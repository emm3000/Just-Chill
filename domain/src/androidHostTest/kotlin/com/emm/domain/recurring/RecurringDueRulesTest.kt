package com.emm.domain.recurring

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    @Test
    fun `parsePeriodKey round-trips a period key`() {
        assertEquals(YearMonth(2026, Month.FEBRUARY), parsePeriodKey("2026-02"))
        assertEquals(YearMonth(2026, Month.DECEMBER), parsePeriodKey("2026-12"))
    }

    @Test
    fun `parsePeriodKey returns null on malformed input`() {
        // Rows arrive from other devices, so the stored key is untrusted like any other column.
        assertNull(parsePeriodKey("2026"))
        assertNull(parsePeriodKey("2026-13"))
        assertNull(parsePeriodKey("2026-00"))
        assertNull(parsePeriodKey("abcd-ef"))
        assertNull(parsePeriodKey(""))
    }

    /**
     * A year `periodKey` could never have written is malformed, and it jams the template it is on.
     *
     * `ensureNotSettled` compares this column as a plain string, so `"12345-07"` sorts above every
     * real key: every confirmation is refused as already-settled while `pendingPeriods` reads the
     * same value as a floor past the current month and offers nothing to confirm. The template goes
     * silent, permanently, and the import that was meant to repair marks like this passed it
     * straight through because nothing bounded the year.
     *
     * `"9999-12"` sorts above every real key too and is deliberately still accepted: it is a mark
     * `periodKey` can write, and "settled through December 9999" refusing confirmations is the
     * monotonic guard doing its job on a value someone meant. Only the unwritable one is a defect.
     */
    @Test
    fun `parsePeriodKey rejects a year no periodKey could have written`() {
        assertNull(parsePeriodKey("12345-07"))
        assertNull(parsePeriodKey("0-07"))
        assertNull(parsePeriodKey("0000-07"))
        assertEquals(YearMonth(9999, Month.DECEMBER), parsePeriodKey("9999-12"))
    }

    /**
     * The same rejection, for the years a *four-character* check let through.
     *
     * `periodKey` pads the month and not the year, so a four-character year it emits is only ever
     * `1000..9999`. Bounding by length accepted `"0999"` anyway, and that
     * is worse than the five-digit case, not better: the import parses it as year 999 and re-encodes
     * through `periodKey` into `"999-07"`, which this parser then rejects (three characters). The
     * column ends up holding a mark `pendingPeriods` reads as "never settled" — so it lists those
     * months as owed — while `ensureNotSettled`'s raw string comparison reads `"999-07" >= "2026-08"`
     * as settled and refuses **both** confirm and skip. There is no tap left that unjams it.
     *
     * `"+999"` is the same hole through a different door: `toIntOrNull` accepts a leading sign, so it
     * is four characters and parses to 999.
     */
    @Test
    fun `parsePeriodKey rejects a four-character year periodKey would never have padded`() {
        assertNull(parsePeriodKey("0999-07"))
        assertNull(parsePeriodKey("0001-01"))
        assertNull(parsePeriodKey("+999-07"))
    }

    @Test
    fun `parsePeriodKey accepts the first year periodKey can write without padding`() {
        // The floor is inclusive: 1000 is the smallest year that renders as four characters, so it
        // is a key periodKey can emit and the bound must not round it away with the padded ones.
        assertEquals(YearMonth(1000, Month.JANUARY), parsePeriodKey("1000-01"))
    }

    @Test
    fun `parsePeriodKey still accepts a month written without its leading zero`() {
        // Deliberate leniency, and the year bound must not take it away: the import repairs
        // "2026-7" — which sorts above "2026-08" and would settle August by accident — only by
        // parsing it and re-encoding through periodKey, which needs the parser to read it first.
        assertEquals(YearMonth(2026, Month.JULY), parsePeriodKey("2026-7"))
    }

    // ── pendingPeriods boundaries ─────────────────────────────────────────────

    /**
     * Scenario 2.6 — before due day → nothing owed yet.
     */
    @Test
    fun `pendingPeriods is empty when today is before this month's due day`() {
        // spec 2.6: dayOfMonth=15, today=Feb 5
        val rm = buildTemplate(dayOfMonth = 15, lastConfirmedPeriod = "2026-01")
        assertEquals(emptyList(), pendingPeriods(rm, LocalDate(2026, 2, 5), TimeZone.UTC))
    }

    /**
     * Scenario 2.1 / 3.5 — on the due day → owed.
     */
    @Test
    fun `pendingPeriods includes this month when today equals the due day`() {
        // spec 2.1 & 3.5
        val rm = buildTemplate(dayOfMonth = 15, lastConfirmedPeriod = "2026-01")
        assertEquals(
            listOf(YearMonth(2026, Month.FEBRUARY)),
            pendingPeriods(rm, LocalDate(2026, 2, 15), TimeZone.UTC),
        )
    }

    @Test
    fun `pendingPeriods includes this month when today is after the due day`() {
        val rm = buildTemplate(dayOfMonth = 15, lastConfirmedPeriod = "2026-01")
        assertEquals(
            listOf(YearMonth(2026, Month.FEBRUARY)),
            pendingPeriods(rm, LocalDate(2026, 2, 20), TimeZone.UTC),
        )
    }

    /**
     * Scenario 3.1 — inactive template owes nothing.
     */
    @Test
    fun `pendingPeriods is empty when the template is inactive`() {
        // spec 3.1
        val rm = buildTemplate(dayOfMonth = 1, isActive = false, lastConfirmedPeriod = "2026-01")
        assertEquals(emptyList(), pendingPeriods(rm, LocalDate(2026, 5, 20), TimeZone.UTC))
    }

    /**
     * Scenario 3.2 — settled through this period → nothing owed.
     */
    @Test
    fun `pendingPeriods is empty when the mark is the current period`() {
        // spec 3.2
        val rm = buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = "2026-05")
        assertEquals(emptyList(), pendingPeriods(rm, LocalDate(2026, 5, 20), TimeZone.UTC))
    }

    /**
     * Scenario 3.3 — settled through last month, new month → this month owed.
     */
    @Test
    fun `pendingPeriods returns only this month when the mark is last month`() {
        // spec 3.3
        val rm = buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = "2026-04")
        assertEquals(
            listOf(YearMonth(2026, Month.MAY)),
            pendingPeriods(rm, LocalDate(2026, 5, 1), TimeZone.UTC),
        )
    }

    /**
     * Scenario 3.4 — never confirmed, before due day → this month not owed yet.
     */
    @Test
    fun `pendingPeriods excludes this month when never confirmed and today is before the due day`() {
        // spec 3.4
        val rm = buildTemplate(dayOfMonth = 20, lastConfirmedPeriod = null, createdAt = epochMillis(2026, 5, 1))
        assertEquals(emptyList(), pendingPeriods(rm, LocalDate(2026, 5, 10), TimeZone.UTC))
    }

    /**
     * Scenario 3.5 — never confirmed, on due day → owed.
     */
    @Test
    fun `pendingPeriods includes this month when never confirmed and today equals the due day`() {
        // spec 3.5
        val rm = buildTemplate(dayOfMonth = 20, lastConfirmedPeriod = null, createdAt = epochMillis(2026, 5, 1))
        assertEquals(
            listOf(YearMonth(2026, Month.MAY)),
            pendingPeriods(rm, LocalDate(2026, 5, 20), TimeZone.UTC),
        )
    }

    /**
     * Scenario 2.2 — clamp: day 31 in non-leap Feb → effectiveDay=28; today=28 → owed.
     */
    @Test
    fun `pendingPeriods clamps day 31 to the last day of non-leap February`() {
        // spec 2.2
        val rm = buildTemplate(dayOfMonth = 31, lastConfirmedPeriod = "2026-01")
        assertEquals(emptyList(), pendingPeriods(rm, LocalDate(2026, 2, 27), TimeZone.UTC))
        assertEquals(
            listOf(YearMonth(2026, Month.FEBRUARY)),
            pendingPeriods(rm, LocalDate(2026, 2, 28), TimeZone.UTC),
        )
    }

    /**
     * Scenario 2.3 — clamp: day 31 in leap Feb → effectiveDay=29; today=29 → owed.
     */
    @Test
    fun `pendingPeriods clamps day 31 to the last day of leap February`() {
        // spec 2.3
        val rm = buildTemplate(dayOfMonth = 31, lastConfirmedPeriod = "2024-01")
        assertEquals(
            listOf(YearMonth(2024, Month.FEBRUARY)),
            pendingPeriods(rm, LocalDate(2024, 2, 29), TimeZone.UTC),
        )
    }

    /**
     * Scenario 2.4 — clamp: day 31 in April → effectiveDay=30; today=30 → owed.
     */
    @Test
    fun `pendingPeriods clamps day 31 to April 30`() {
        // spec 2.4
        val rm = buildTemplate(dayOfMonth = 31, lastConfirmedPeriod = "2026-03")
        assertEquals(
            listOf(YearMonth(2026, Month.APRIL)),
            pendingPeriods(rm, LocalDate(2026, 4, 30), TimeZone.UTC),
        )
    }

    // ── catch-up: the whole point of A6 ───────────────────────────────────────

    /**
     * Scenario 7.1, rewritten. Settled through March, today is May 15 — April was missed while the
     * app was closed. It used to be gone for good: the rules only ever looked at the current month,
     * so April's rent could never be recorded. Both months are now owed, oldest first.
     */
    @Test
    fun `pendingPeriods surfaces the month missed while the app was closed`() {
        val rm = buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = "2026-03")

        val result = pendingPeriods(rm, LocalDate(2026, 5, 15), TimeZone.UTC)

        assertEquals(listOf(YearMonth(2026, Month.APRIL), YearMonth(2026, Month.MAY)), result)
    }

    @Test
    fun `a past period is owed regardless of its due day`() {
        // April's 28th is long gone by May 1 — only the current month has to clear the due day.
        val rm = buildTemplate(dayOfMonth = 28, lastConfirmedPeriod = "2026-03")

        val result = pendingPeriods(rm, LocalDate(2026, 5, 1), TimeZone.UTC)

        assertEquals(listOf(YearMonth(2026, Month.APRIL)), result)
    }

    @Test
    fun `pendingPeriods never reaches back before the template existed`() {
        // Created April 10, never confirmed. March is not owed — the template did not exist yet.
        val rm = buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = null, createdAt = epochMillis(2026, 4, 10))

        val result = pendingPeriods(rm, LocalDate(2026, 5, 15), TimeZone.UTC)

        assertEquals(listOf(YearMonth(2026, Month.APRIL), YearMonth(2026, Month.MAY)), result)
    }

    @Test
    fun `pendingPeriods caps the catch-up window`() {
        // Created in 2020 and never confirmed. Without a cap this would list 60-odd months.
        val rm = buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = null, createdAt = epochMillis(2020, 1, 1))

        val result = pendingPeriods(rm, LocalDate(2026, 5, 15), TimeZone.UTC)

        assertEquals(MAX_CATCH_UP_MONTHS, result.size)
        assertEquals(YearMonth(2025, Month.JUNE), result.first())
        assertEquals(YearMonth(2026, Month.MAY), result.last())
    }

    @Test
    fun `pendingPeriods crosses the year boundary`() {
        val rm = buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = "2025-11")

        val result = pendingPeriods(rm, LocalDate(2026, 1, 15), TimeZone.UTC)

        assertEquals(listOf(YearMonth(2025, Month.DECEMBER), YearMonth(2026, Month.JANUARY)), result)
    }

    @Test
    fun `pendingPeriods ignores a malformed stored mark`() {
        // A corrupt key must not silently swallow the catch-up window; createdAt still floors it.
        val rm = buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = "garbage", createdAt = epochMillis(2026, 4, 1))

        val result = pendingPeriods(rm, LocalDate(2026, 5, 15), TimeZone.UTC)

        assertEquals(listOf(YearMonth(2026, Month.APRIL), YearMonth(2026, Month.MAY)), result)
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private fun epochMillis(year: Int, month: Int, day: Int): Long =
        LocalDate(year, month, day).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

    private fun buildTemplate(
        dayOfMonth: Int,
        isActive: Boolean = true,
        lastConfirmedPeriod: String? = null,
        createdAt: Long = epochMillis(2020, 1, 1),
    ) = RecurringMovement(
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
        createdAt = createdAt,
    )
}
