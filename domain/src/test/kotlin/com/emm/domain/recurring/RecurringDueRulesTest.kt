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

class RecurringDueRulesTest {

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

    @Test
    fun `effectiveDueDay does not clamp day 15 in non-leap Feb`() {
        val result = effectiveDueDay(15, YearMonth(2026, Month.FEBRUARY))
        assertEquals(15, result)
    }

    @Test
    fun `effectiveDueDay clamps day 31 to 28 in non-leap February`() {
        val result = effectiveDueDay(31, YearMonth(2026, Month.FEBRUARY))
        assertEquals(28, result)
    }

    @Test
    fun `effectiveDueDay clamps day 31 to 29 in leap February`() {
        val result = effectiveDueDay(31, YearMonth(2024, Month.FEBRUARY))
        assertEquals(29, result)
    }

    @Test
    fun `effectiveDueDay clamps day 31 to 30 in April`() {
        val result = effectiveDueDay(31, YearMonth(2026, Month.APRIL))
        assertEquals(30, result)
    }

    @Test
    fun `effectiveDueDay returns 1 for day 1 any month`() {
        assertEquals(1, effectiveDueDay(1, YearMonth(2026, Month.FEBRUARY)))
        assertEquals(1, effectiveDueDay(1, YearMonth(2026, Month.MARCH)))
    }

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
        assertNull(parsePeriodKey("2026"))
        assertNull(parsePeriodKey("2026-13"))
        assertNull(parsePeriodKey("2026-00"))
        assertNull(parsePeriodKey("abcd-ef"))
        assertNull(parsePeriodKey(""))
    }

    @Test
    fun `parsePeriodKey rejects a year no periodKey could have written`() {
        assertNull(parsePeriodKey("12345-07"))
        assertNull(parsePeriodKey("0-07"))
        assertNull(parsePeriodKey("0000-07"))
        assertEquals(YearMonth(9999, Month.DECEMBER), parsePeriodKey("9999-12"))
    }

    @Test
    fun `parsePeriodKey rejects a four-character year periodKey would never have padded`() {
        assertNull(parsePeriodKey("0999-07"))
        assertNull(parsePeriodKey("0001-01"))
        assertNull(parsePeriodKey("+999-07"))
    }

    @Test
    fun `parsePeriodKey accepts the first year periodKey can write without padding`() {
        assertEquals(YearMonth(1000, Month.JANUARY), parsePeriodKey("1000-01"))
    }

    @Test
    fun `parsePeriodKey still accepts a month written without its leading zero`() {
        assertEquals(YearMonth(2026, Month.JULY), parsePeriodKey("2026-7"))
    }

    @Test
    fun `pendingPeriods is empty when today is before this month's due day`() {
        val rm = buildTemplate(dayOfMonth = 15, lastConfirmedPeriod = "2026-01")
        assertEquals(emptyList(), pendingPeriods(rm, LocalDate(2026, 2, 5), TimeZone.UTC))
    }

    @Test
    fun `pendingPeriods includes this month when today equals the due day`() {
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

    @Test
    fun `pendingPeriods is empty when the template is inactive`() {
        val rm = buildTemplate(dayOfMonth = 1, isActive = false, lastConfirmedPeriod = "2026-01")
        assertEquals(emptyList(), pendingPeriods(rm, LocalDate(2026, 5, 20), TimeZone.UTC))
    }

    @Test
    fun `pendingPeriods is empty when the mark is the current period`() {
        val rm = buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = "2026-05")
        assertEquals(emptyList(), pendingPeriods(rm, LocalDate(2026, 5, 20), TimeZone.UTC))
    }

    @Test
    fun `pendingPeriods returns only this month when the mark is last month`() {
        val rm = buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = "2026-04")
        assertEquals(
            listOf(YearMonth(2026, Month.MAY)),
            pendingPeriods(rm, LocalDate(2026, 5, 1), TimeZone.UTC),
        )
    }

    @Test
    fun `pendingPeriods excludes this month when never confirmed and today is before the due day`() {
        val rm = buildTemplate(dayOfMonth = 20, lastConfirmedPeriod = null, createdAt = epochMillis(2026, 5, 1))
        assertEquals(emptyList(), pendingPeriods(rm, LocalDate(2026, 5, 10), TimeZone.UTC))
    }

    @Test
    fun `pendingPeriods includes this month when never confirmed and today equals the due day`() {
        val rm = buildTemplate(dayOfMonth = 20, lastConfirmedPeriod = null, createdAt = epochMillis(2026, 5, 1))
        assertEquals(
            listOf(YearMonth(2026, Month.MAY)),
            pendingPeriods(rm, LocalDate(2026, 5, 20), TimeZone.UTC),
        )
    }

    @Test
    fun `pendingPeriods clamps day 31 to the last day of non-leap February`() {
        val rm = buildTemplate(dayOfMonth = 31, lastConfirmedPeriod = "2026-01")
        assertEquals(emptyList(), pendingPeriods(rm, LocalDate(2026, 2, 27), TimeZone.UTC))
        assertEquals(
            listOf(YearMonth(2026, Month.FEBRUARY)),
            pendingPeriods(rm, LocalDate(2026, 2, 28), TimeZone.UTC),
        )
    }

    @Test
    fun `pendingPeriods clamps day 31 to the last day of leap February`() {
        val rm = buildTemplate(dayOfMonth = 31, lastConfirmedPeriod = "2024-01")
        assertEquals(
            listOf(YearMonth(2024, Month.FEBRUARY)),
            pendingPeriods(rm, LocalDate(2024, 2, 29), TimeZone.UTC),
        )
    }

    @Test
    fun `pendingPeriods clamps day 31 to April 30`() {
        val rm = buildTemplate(dayOfMonth = 31, lastConfirmedPeriod = "2026-03")
        assertEquals(
            listOf(YearMonth(2026, Month.APRIL)),
            pendingPeriods(rm, LocalDate(2026, 4, 30), TimeZone.UTC),
        )
    }

    @Test
    fun `pendingPeriods surfaces the month missed while the app was closed`() {
        val rm = buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = "2026-03")

        val result = pendingPeriods(rm, LocalDate(2026, 5, 15), TimeZone.UTC)

        assertEquals(listOf(YearMonth(2026, Month.APRIL), YearMonth(2026, Month.MAY)), result)
    }

    @Test
    fun `a past period is owed regardless of its due day`() {
        val rm = buildTemplate(dayOfMonth = 28, lastConfirmedPeriod = "2026-03")

        val result = pendingPeriods(rm, LocalDate(2026, 5, 1), TimeZone.UTC)

        assertEquals(listOf(YearMonth(2026, Month.APRIL)), result)
    }

    @Test
    fun `pendingPeriods never reaches back before the template existed`() {
        val rm = buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = null, createdAt = epochMillis(2026, 4, 10))

        val result = pendingPeriods(rm, LocalDate(2026, 5, 15), TimeZone.UTC)

        assertEquals(listOf(YearMonth(2026, Month.APRIL), YearMonth(2026, Month.MAY)), result)
    }

    @Test
    fun `pendingPeriods caps the catch-up window`() {
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
        val rm = buildTemplate(dayOfMonth = 1, lastConfirmedPeriod = "garbage", createdAt = epochMillis(2026, 4, 1))

        val result = pendingPeriods(rm, LocalDate(2026, 5, 15), TimeZone.UTC)

        assertEquals(listOf(YearMonth(2026, Month.APRIL), YearMonth(2026, Month.MAY)), result)
    }

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
