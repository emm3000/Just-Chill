package com.emm.domain.shared.backup

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class SnapshotRetentionTest {

    @Test
    fun `an empty bucket decides nothing`() {
        val decision = SnapshotRetention.select(emptyList(), NOW, LIMA)

        assertEquals(emptyList(), decision.keep)
        assertEquals(emptyList(), decision.delete)
    }

    @Test
    fun `fewer snapshots than any bucket holds means nothing is deleted`() {
        val candidates = snapshots("2026-08-12", "2026-08-13", "2026-08-14")

        val decision = SnapshotRetention.select(candidates, NOW, LIMA)

        assertEquals(candidates.map { it.id }, decision.keep)
        assertEquals(emptyList(), decision.delete)
    }

    @Test
    fun `seven consecutive days all fit the daily slots`() {
        val days = consecutiveDaysTo("2026-08-14", SnapshotRetention.DAILY_SLOTS)

        assertEquals(emptyList(), SnapshotRetention.select(days, NOW, LIMA).delete)
    }

    @Test
    fun `the eighth consecutive day falls out, because no other bucket is keeping it`() {
        val days = consecutiveDaysTo("2026-08-14", SnapshotRetention.DAILY_SLOTS + 1)

        val decision = SnapshotRetention.select(days, NOW, LIMA)

        assertEquals(listOf(id("2026-08-07")), decision.delete)
        assertEquals(days.size - 1, decision.keep.size)
    }

    @Test
    fun `eight weekly snapshots all fit the weekly slots`() {
        val weeks = snapshots(*MONDAYS.take(SnapshotRetention.WEEKLY_SLOTS).toTypedArray())

        assertEquals(emptyList(), SnapshotRetention.select(weeks, NOW, LIMA).delete)
    }

    @Test
    fun `the ninth weekly snapshot falls out`() {
        val decision = SnapshotRetention.select(snapshots(*MONDAYS.toTypedArray()), NOW, LIMA)

        assertEquals(listOf(id("2026-06-15")), decision.delete)
    }

    @Test
    fun `twelve monthly snapshots all fit the monthly slots`() {
        val months = snapshots(*FIFTEENTHS.take(SnapshotRetention.MONTHLY_SLOTS).toTypedArray())

        assertEquals(emptyList(), SnapshotRetention.select(months, NOW, LIMA).delete)
    }

    @Test
    fun `the thirteenth monthly snapshot falls out`() {
        val decision = SnapshotRetention.select(snapshots(*FIFTEENTHS.toTypedArray()), NOW, LIMA)

        assertEquals(listOf(id("2025-08-15")), decision.delete)
    }

    @Test
    fun `two snapshots on one calendar day share one daily slot and the later one takes it`() {
        val morning = candidate("2026-08-14T13:00:00Z")
        val evening = candidate("2026-08-14T21:00:00Z")

        val decision = SnapshotRetention.select(listOf(morning, evening), NOW, LIMA)

        assertEquals(listOf(evening.id), decision.keep)
        assertEquals(listOf(morning.id), decision.delete)
    }

    @Test
    fun `a snapshot no daily slot holds survives as its week's and its month's newest`() {
        val recent = consecutiveDaysTo("2026-08-14", TEN)
        val lonely = snapshots("2026-06-03")

        val decision = SnapshotRetention.select(recent + lonely, NOW, LIMA)

        assertTrue(id("2026-06-03") in decision.keep, "the only snapshot of its week and month was deleted")
        assertEquals(listOf(id("2026-08-05"), id("2026-08-06"), id("2026-08-07")), decision.delete)
    }

    @Test
    fun `the same instants bucket differently in two zones, and the injected one wins`() {
        val nearMidnight = listOf(candidate("2026-08-14T02:00:00Z"), candidate("2026-08-14T20:00:00Z"))

        val inUtc = SnapshotRetention.select(nearMidnight, NOW, TimeZone.UTC)

        assertEquals(emptyList(), SnapshotRetention.select(nearMidnight, NOW, LIMA).delete)
        assertEquals(listOf(id("2026-08-14T02:00:00Z")), inUtc.delete)
    }

    @Test
    fun `a snapshot stamped in the future is kept and fills no slot`() {
        val days = consecutiveDaysTo("2026-08-14", SnapshotRetention.DAILY_SLOTS + 1)
        val skewed = candidate("2026-09-20T12:00:00Z")

        val decision = SnapshotRetention.select(days + skewed, Instant.parse("2026-08-15T00:00:00Z"), LIMA)

        assertTrue(skewed.id in decision.keep)
        assertEquals(listOf(id("2026-08-07")), decision.delete)
    }

    @Test
    fun `a crowd of future-stamped snapshots collapses into the same slots instead of piling up`() {
        val skewed = consecutiveDaysTo("2029-01-01", MANY)

        val decision = SnapshotRetention.select(skewed, NOW, LIMA)

        assertTrue(decision.keep.size <= SLOTS_PER_SHELF, "the future shelf kept ${decision.keep.size}")
        assertEquals(MANY - decision.keep.size, decision.delete.size)
        assertTrue(id("2029-01-01") in decision.keep, "the newest future-stamped snapshot was deleted")
    }

    @Test
    fun `a crowd of future-stamped snapshots still evicts nothing that is eligible`() {
        val days = consecutiveDaysTo("2026-08-14", SnapshotRetention.DAILY_SLOTS + 1)
        val skewed = consecutiveDaysTo("2029-01-01", MANY)
        val eligibleIds: Set<String> = days.map { it.id }.toSet()

        val alone = SnapshotRetention.select(days, NOW, LIMA)
        val crowded = SnapshotRetention.select(days + skewed, NOW, LIMA)

        assertEquals(alone.keep, crowded.keep.filter { it in eligibleIds })
        assertEquals(alone.delete, crowded.delete.filter { it in eligibleIds })
    }

    @Test
    fun `the survivor count is bounded whatever a broken clock does`() {
        val eligible = consecutiveDaysTo("2026-08-14", MANY)
        val skewed = consecutiveDaysTo("2029-01-01", MANY)

        val decision = SnapshotRetention.select(eligible + skewed, NOW, LIMA)

        assertTrue(decision.keep.size <= 2 * SLOTS_PER_SHELF, "kept ${decision.keep.size}")
    }

    @Test
    fun `the answer is a function of the input set, not of the order it arrives in`() {
        val candidates = snapshots(*FIFTEENTHS.toTypedArray()) + consecutiveDaysTo("2026-08-14", 2)

        val straight = SnapshotRetention.select(candidates, NOW, LIMA)
        val shuffled = SnapshotRetention.select(candidates.reversed(), NOW, LIMA)

        assertEquals(straight, shuffled)
        assertEquals(candidates.size, straight.keep.size + straight.delete.size)
        assertEquals(candidates.map { it.id }.toSet(), (straight.keep + straight.delete).toSet())
    }

    @Test
    fun `the same input decided twice gives the same answer, because nothing here reads a clock`() {
        val candidates = snapshots(*FIFTEENTHS.toTypedArray())

        assertEquals(
            SnapshotRetention.select(candidates, NOW, LIMA),
            SnapshotRetention.select(candidates, NOW, LIMA),
        )
    }
}

private fun id(day: String): String = if (day.contains('T')) day else "${day}T12:00:00Z"

private fun candidate(instant: String): RetentionCandidate =
    RetentionCandidate(id(instant), Instant.parse(id(instant)).toEpochMilliseconds())

private fun snapshots(vararg days: String): List<RetentionCandidate> = days.map(::candidate)

private val LIMA: TimeZone = TimeZone.of("America/Lima")

private val NOW: Instant = Instant.parse("2027-01-01T00:00:00Z")

private const val TEN = 10

private const val MANY = 400

private val SLOTS_PER_SHELF: Int =
    SnapshotRetention.DAILY_SLOTS + SnapshotRetention.WEEKLY_SLOTS + SnapshotRetention.MONTHLY_SLOTS

private fun consecutiveDaysTo(endInclusive: String, count: Int): List<RetentionCandidate> =
    snapshots(*stepBackFrom(endInclusive, count, DateTimeUnit.DAY).reversed().toTypedArray())

private fun stepBackFrom(endInclusive: String, count: Int, unit: DateTimeUnit.DateBased): List<String> =
    List(count) { LocalDate.parse(endInclusive).minus(it, unit).toString() }

private val MONDAYS = stepBackFrom("2026-08-10", SnapshotRetention.WEEKLY_SLOTS + 1, DateTimeUnit.WEEK)

private val FIFTEENTHS = stepBackFrom("2026-08-15", SnapshotRetention.MONTHLY_SLOTS + 1, DateTimeUnit.MONTH)
