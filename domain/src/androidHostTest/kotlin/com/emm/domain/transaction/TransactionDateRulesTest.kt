package com.emm.domain.transaction

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * A transaction records money that already moved, so its day cannot be after today.
 *
 * The comparison is between calendar DAYS, not instants. Comparing instants would make the rule
 * depend on the time of day a transaction carries — and on the Edit path that time comes from the
 * original transaction, not from now.
 */
class TransactionDateRulesTest {

    private val lima = TimeZone.of("America/Lima")

    private fun clockAt(date: LocalDate, hour: Int, minute: Int, zone: TimeZone): Clock = object : Clock {
        override fun now(): Instant = LocalDateTime(date, LocalTime(hour, minute)).toInstant(zone)
    }

    private fun millisAt(date: LocalDate, hour: Int, minute: Int, zone: TimeZone): Long =
        LocalDateTime(date, LocalTime(hour, minute)).toInstant(zone).toEpochMilliseconds()

    private val today = LocalDate(2026, Month.AUGUST, 11)

    @Test
    fun `today is accepted`() {
        val clock = clockAt(today, hour = 9, minute = 0, zone = lima)

        ensureNotFutureDated(today.atStartOfDayIn(lima).toEpochMilliseconds(), clock, lima)
    }

    @Test
    fun `a past day is accepted`() {
        val clock = clockAt(today, hour = 9, minute = 0, zone = lima)
        val lastYear = LocalDate(2025, Month.DECEMBER, 31).atStartOfDayIn(lima).toEpochMilliseconds()

        ensureNotFutureDated(lastYear, clock, lima)
    }

    @Test
    fun `tomorrow is rejected with a machine-readable code`() {
        val clock = clockAt(today, hour = 9, minute = 0, zone = lima)
        val tomorrow = LocalDate(2026, Month.AUGUST, 12).atStartOfDayIn(lima).toEpochMilliseconds()

        val ex = assertFailsWith<DomainException.ValidationError> {
            ensureNotFutureDated(tomorrow, clock, lima)
        }
        assertEquals(ValidationCode.DateInTheFuture, ex.code)
    }

    @Test
    fun `later today is accepted — the rule compares days, not instants`() {
        // 09:00 now, transaction stamped 23:00 today. An instant comparison would reject this; the
        // Edit path reaches it by keeping the original transaction's time of day.
        val clock = clockAt(today, hour = 9, minute = 0, zone = lima)

        ensureNotFutureDated(millisAt(today, hour = 23, minute = 0, zone = lima), clock, lima)
    }

    @Test
    fun `the day is resolved in the given zone, not in UTC`() {
        // 11 Aug 22:00 in Lima is 12 Aug 03:00 UTC. Read in UTC the transaction looks like tomorrow
        // and would be rejected; the user is in Lima and it is today.
        val clock = clockAt(today, hour = 22, minute = 30, zone = lima)

        ensureNotFutureDated(millisAt(today, hour = 22, minute = 0, zone = lima), clock, lima)
    }
}
