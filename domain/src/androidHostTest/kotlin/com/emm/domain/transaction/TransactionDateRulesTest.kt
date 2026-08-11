package com.emm.domain.transaction

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * A transaction records money that already moved, so its day cannot be after today.
 *
 * The comparison is between calendar DAYS, not the whole value. Comparing the whole value would
 * make the rule depend on the time of day a transaction carries — and on the Edit path that time
 * comes from the original transaction, not from now.
 *
 * The clock and the zone are read for exactly one thing here: what today's local date is. The
 * value being checked carries no zone of its own, so there is nothing to convert.
 */
class TransactionDateRulesTest {

    private val lima = TimeZone.of("America/Lima")

    /** Ahead of Lima by half a day: 11 Aug 22:00 in Lima is already 12 Aug here. */
    private val karachi = TimeZone.of("Asia/Karachi")

    private fun clockAt(date: LocalDate, hour: Int, minute: Int, zone: TimeZone): Clock = object : Clock {
        override fun now(): Instant = LocalDateTime(date, LocalTime(hour, minute)).toInstant(zone)
    }

    private val today = LocalDate(2026, Month.AUGUST, 11)

    private fun at(date: LocalDate, hour: Int = 0, minute: Int = 0) = LocalDateTime(date, LocalTime(hour, minute))

    @Test
    fun `today is accepted`() {
        val clock = clockAt(today, hour = 9, minute = 0, zone = lima)

        ensureNotFutureDated(at(today), clock, lima)
    }

    @Test
    fun `a past day is accepted`() {
        val clock = clockAt(today, hour = 9, minute = 0, zone = lima)

        ensureNotFutureDated(at(LocalDate(2025, Month.DECEMBER, 31)), clock, lima)
    }

    @Test
    fun `tomorrow is rejected with a machine-readable code`() {
        val clock = clockAt(today, hour = 9, minute = 0, zone = lima)

        val ex = assertFailsWith<DomainException.ValidationError> {
            ensureNotFutureDated(at(LocalDate(2026, Month.AUGUST, 12)), clock, lima)
        }
        assertEquals(ValidationCode.DateInTheFuture, ex.code)
    }

    @Test
    fun `later today is accepted — the rule compares days, not the time of day`() {
        // 09:00 now, transaction stamped 23:00 today. Comparing the whole value would reject this;
        // the Edit path reaches it by keeping the original transaction's time of day.
        val clock = clockAt(today, hour = 9, minute = 0, zone = lima)

        ensureNotFutureDated(at(today, hour = 23), clock, lima)
    }

    @Test
    fun `today is whatever the user's zone says it is`() {
        // The same instant: 11 Aug 22:00 in Lima, which is already 12 Aug 08:00 in Karachi. A
        // transaction dated the 12th is the future for the user in Lima and the present for the
        // user in Karachi, and only the zone can tell the two apart.
        val instant = LocalDateTime(today, LocalTime(22, 0)).toInstant(lima)
        val clock = object : Clock {
            override fun now(): Instant = instant
        }
        val twelfth = at(LocalDate(2026, Month.AUGUST, 12), hour = 10)

        ensureNotFutureDated(twelfth, clock, karachi)
        assertFailsWith<DomainException.ValidationError> { ensureNotFutureDated(twelfth, clock, lima) }
    }
}
