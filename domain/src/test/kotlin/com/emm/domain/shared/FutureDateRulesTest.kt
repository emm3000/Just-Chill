package com.emm.domain.shared

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

class FutureDateRulesTest {

    private val lima = TimeZone.of("America/Lima")

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
        val clock = clockAt(today, hour = 9, minute = 0, zone = lima)

        ensureNotFutureDated(at(today, hour = 23), clock, lima)
    }

    @Test
    fun `today is whatever the user's zone says it is`() {
        val instant = LocalDateTime(today, LocalTime(22, 0)).toInstant(lima)
        val clock = object : Clock {
            override fun now(): Instant = instant
        }
        val twelfth = at(LocalDate(2026, Month.AUGUST, 12), hour = 10)

        ensureNotFutureDated(twelfth, clock, karachi)
        assertFailsWith<DomainException.ValidationError> { ensureNotFutureDated(twelfth, clock, lima) }
    }
}
