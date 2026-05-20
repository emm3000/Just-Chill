package com.emm.domain.shared

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DateAndTimeCombinerTest {

    private val combiner = DateAndTimeCombiner()

    @Test
    fun `combineWithUtc preserves the date when interpreted as UTC`() {
        val targetDate = LocalDate(2026, 5, 15)
        val dateInMillis = targetDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

        val result = combiner.combineWithUtc(dateInMillis)

        val zone = TimeZone.currentSystemDefault()
        val resultDate = Instant.fromEpochMilliseconds(result).toLocalDateTime(zone).date
        assertEquals(targetDate, resultDate)
    }

    @Test
    fun `combineWithUtc uses current wall-clock time`() {
        val zone = TimeZone.currentSystemDefault()
        val dateInMillis = LocalDate(2026, 5, 15).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

        val beforeMillis = Clock.System.now().toEpochMilliseconds()
        val result = combiner.combineWithUtc(dateInMillis)
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
}
