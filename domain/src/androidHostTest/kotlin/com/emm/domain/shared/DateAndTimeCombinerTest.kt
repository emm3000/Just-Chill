package com.emm.domain.shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class DateAndTimeCombinerTest {

    private val combiner = DateAndTimeCombiner()

    /** UTC-5. Local evenings fall on the next UTC day. */
    private val lima = TimeZone.of("America/Lima")

    /** UTC+5. Local midnight falls on the previous UTC day. */
    private val karachi = TimeZone.of("Asia/Karachi")

    @Test
    fun `combineWithCurrentTime preserves the date the picker selected`() {
        val targetDate = LocalDate(2026, 5, 15)
        val dateInMillis = targetDate.atStartOfDayIn(lima).toEpochMilliseconds()

        val result = DateAndTimeCombiner(lima).combineWithCurrentTime(dateInMillis)

        assertEquals(targetDate, result.dateIn(lima))
    }

    @Test
    fun `combineWithCurrentTime is idempotent for a stored evening timestamp`() {
        val storedDate = LocalDate(2026, 5, 15)
        val storedEvening = LocalDateTime(storedDate, LocalTime(21, 48))
            .toInstant(lima)
            .toEpochMilliseconds()

        val result = DateAndTimeCombiner(lima).combineWithCurrentTime(storedEvening)

        assertEquals(storedDate, result.dateIn(lima))
    }

    @Test
    fun `combineWithCurrentTime preserves the date in zones ahead of UTC`() {
        val targetDate = LocalDate(2026, 5, 15)
        val dateInMillis = targetDate.atStartOfDayIn(karachi).toEpochMilliseconds()

        val result = DateAndTimeCombiner(karachi).combineWithCurrentTime(dateInMillis)

        assertEquals(targetDate, result.dateIn(karachi))
    }

    @Test
    fun `combineWithCurrentTime uses current wall-clock time`() {
        val zone = TimeZone.currentSystemDefault()
        val dateInMillis = LocalDate(2026, 5, 15).atStartOfDayIn(zone).toEpochMilliseconds()

        val beforeMillis = Clock.System.now().toEpochMilliseconds()
        val result = combiner.combineWithCurrentTime(dateInMillis)
        val afterMillis = Clock.System.now().toEpochMilliseconds()

        val resultTime = Instant.fromEpochMilliseconds(result).toLocalDateTime(zone).time
        val beforeTime = Instant.fromEpochMilliseconds(beforeMillis).toLocalDateTime(zone).time
        val afterTime = Instant.fromEpochMilliseconds(afterMillis).toLocalDateTime(zone).time

        val toleranceSeconds = 5
        val beforeWithTolerance = beforeTime.toSecondOfDay() - toleranceSeconds
        val afterWithTolerance = afterTime.toSecondOfDay() + toleranceSeconds

        assertTrue(
            resultTime.toSecondOfDay() in beforeWithTolerance..afterWithTolerance,
            "Expected result time $resultTime to be within ±${toleranceSeconds}s of [$beforeTime, $afterTime]",
        )
    }

    @Test
    fun `combineKeepingTimeOf moves the day and carries the original time along`() {
        val original = LocalDateTime(LocalDate(2026, 5, 15), LocalTime(13, 5))
            .toInstant(lima)
            .toEpochMilliseconds()
        val newDay = LocalDate(2026, 5, 20).atStartOfDayIn(lima).toEpochMilliseconds()

        val result = DateAndTimeCombiner(lima).combineKeepingTimeOf(newDay, original)

        assertEquals(LocalDateTime(LocalDate(2026, 5, 20), LocalTime(13, 5)), result.dateTimeIn(lima))
    }

    @Test
    fun `combineKeepingTimeOf returns the very same instant when the day did not change`() {
        val stored = LocalDateTime(LocalDate(2026, 5, 15), LocalTime(21, 48))
            .toInstant(lima)
            .toEpochMilliseconds()

        val result = DateAndTimeCombiner(lima).combineKeepingTimeOf(stored, stored)

        assertEquals(stored, result)
    }

    @Test
    fun `combineKeepingTimeOf ignores the time carried by the day argument`() {
        val original = LocalDateTime(LocalDate(2026, 5, 15), LocalTime(8, 30))
            .toInstant(lima)
            .toEpochMilliseconds()
        val newDayAtNoon = LocalDateTime(LocalDate(2026, 5, 20), LocalTime(12, 0))
            .toInstant(lima)
            .toEpochMilliseconds()

        val result = DateAndTimeCombiner(lima).combineKeepingTimeOf(newDayAtNoon, original)

        assertEquals(LocalDateTime(LocalDate(2026, 5, 20), LocalTime(8, 30)), result.dateTimeIn(lima))
    }

    private fun Long.dateIn(zone: TimeZone): LocalDate = dateTimeIn(zone).date

    private fun Long.dateTimeIn(zone: TimeZone): LocalDateTime =
        Instant.fromEpochMilliseconds(this).toLocalDateTime(zone)
}
