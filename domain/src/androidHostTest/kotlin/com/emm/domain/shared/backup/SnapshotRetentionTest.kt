package com.emm.domain.shared.backup

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * The half of ADR 009 Phase 2c's retention prune where an off-by-one deletes somebody's financial
 * history, tested exhaustively because here it is cheap: ids in, ids out, no storage anywhere.
 *
 * Fixtures are stamped at **12:00 UTC** so a Lima reading (UTC-5) lands on the same calendar day,
 * which keeps every test except the timezone ones about retention rather than about arithmetic. The
 * one week-day fact everything leans on: **2026-08-10 is a Monday** — the same date
 * `DayGroupTest` pins as "Martes 11" for the 11th.
 */
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
        // 08-07 (Friday) is not the newest of its ISO week — 08-09 is — and not the newest of August.
        // Nothing keeps it, so this is the daily boundary showing up as an actual deletion rather
        // than as a snapshot one of the coarser buckets quietly rescues.
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
        // Nine Mondays: the daily slots take the seven newest, the monthly slots take the newest of
        // June, July and August, and the weekly slots take eight of the nine weeks. 06-15 is the one
        // no bucket is holding.
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
        // The grandfather-father-son bug, as a named test: 06-03 is far outside the seven daily
        // slots, and three passes that each deleted what they did not personally want would remove
        // it. Keeping is a UNION, so the daily pass has no vote on what the monthly pass holds.
        val recent = consecutiveDaysTo("2026-08-14", TEN)
        val lonely = snapshots("2026-06-03")

        val decision = SnapshotRetention.select(recent + lonely, NOW, LIMA)

        assertTrue(id("2026-06-03") in decision.keep, "the only snapshot of its week and month was deleted")
        assertEquals(listOf(id("2026-08-05"), id("2026-08-06"), id("2026-08-07")), decision.delete)
    }

    @Test
    fun `the same instants bucket differently in two zones, and the injected one wins`() {
        // 02:00Z is still the previous evening in Lima and already the next morning in UTC. One zone
        // sees two snapshots on one day and drops the older; the other sees two days and keeps both.
        // A test that ran in one zone only would agree with the bug on any machine sitting in it.
        val nearMidnight = listOf(candidate("2026-08-14T02:00:00Z"), candidate("2026-08-14T20:00:00Z"))

        val inUtc = SnapshotRetention.select(nearMidnight, NOW, TimeZone.UTC)

        assertEquals(emptyList(), SnapshotRetention.select(nearMidnight, NOW, LIMA).delete)
        assertEquals(listOf(id("2026-08-14T02:00:00Z")), inUtc.delete)
    }

    @Test
    fun `a snapshot stamped in the future is kept and fills no slot`() {
        // Only a skewed clock produces one. Letting it claim the newest slot of all three buckets
        // would evict three real snapshots to make room for a mistake; keeping it costs one object.
        val days = consecutiveDaysTo("2026-08-14", SnapshotRetention.DAILY_SLOTS + 1)
        val skewed = candidate("2026-09-20T12:00:00Z")

        val decision = SnapshotRetention.select(days + skewed, Instant.parse("2026-08-15T00:00:00Z"), LIMA)

        assertTrue(skewed.id in decision.keep)
        // Unchanged from the same fixture without it: the skewed object took no slot from anybody.
        assertEquals(listOf(id("2026-08-07")), decision.delete)
    }

    @Test
    fun `a crowd of future-stamped snapshots collapses into the same slots instead of piling up`() {
        // The failure this closes is not hypothetical: a clock that jumps forward writes snapshots
        // stamped in the future, the clock is corrected, and every one of those objects fills no
        // slot — so nothing can ever evict them, and each further excursion strands more. Unbounded
        // growth whose only signal is uploads eventually failing.
        val skewed = consecutiveDaysTo("2029-01-01", MANY)

        val decision = SnapshotRetention.select(skewed, NOW, LIMA)

        assertTrue(decision.keep.size <= SLOTS_PER_SHELF, "the future shelf kept ${decision.keep.size}")
        // And the rest are actually deleted rather than merely uncounted: the two lists partition.
        assertEquals(MANY - decision.keep.size, decision.delete.size)
        // Newest first out of a bucket, on this shelf as on the other one.
        assertTrue(id("2029-01-01") in decision.keep, "the newest future-stamped snapshot was deleted")
    }

    @Test
    fun `a crowd of future-stamped snapshots still evicts nothing that is eligible`() {
        // The property the future partition exists for, now that the partition is capped: however
        // many skewed objects arrive, the eligible answer is the answer they were never part of.
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
        // The bound the KDoc states, asserted rather than argued: two shelves of 7 + 8 + 12, and
        // nothing survives without occupying a slot on one of them.
        val eligible = consecutiveDaysTo("2026-08-14", MANY)
        val skewed = consecutiveDaysTo("2029-01-01", MANY)

        val decision = SnapshotRetention.select(eligible + skewed, NOW, LIMA)

        assertTrue(decision.keep.size <= 2 * SLOTS_PER_SHELF, "kept ${decision.keep.size}")
    }

    @Test
    fun `the answer is a function of the input set, not of the order it arrives in`() {
        // FIFTEENTHS and a handful of recent days: disjoint, so no id appears twice and the
        // partition assertion below is about the policy rather than about the fixture.
        val candidates = snapshots(*FIFTEENTHS.toTypedArray()) + consecutiveDaysTo("2026-08-14", 2)

        val straight = SnapshotRetention.select(candidates, NOW, LIMA)
        val shuffled = SnapshotRetention.select(candidates.reversed(), NOW, LIMA)

        assertEquals(straight, shuffled)
        // And nothing is lost or counted twice: the two lists partition the input exactly, which is
        // what makes "kept by one bucket, deleted by another" inexpressible rather than untested.
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

/** Noon UTC so the Lima reading of the same instant lands on the same calendar day. */
private fun id(day: String): String = if (day.contains('T')) day else "${day}T12:00:00Z"

private fun candidate(instant: String): RetentionCandidate =
    RetentionCandidate(id(instant), Instant.parse(id(instant)).toEpochMilliseconds())

/** The id IS the timestamp, so an assertion failure names the snapshot rather than an index. */
private fun snapshots(vararg days: String): List<RetentionCandidate> = days.map(::candidate)

private val LIMA: TimeZone = TimeZone.of("America/Lima")

/** Well past every fixture, so nothing counts as future-dated unless a test says so. */
private val NOW: Instant = Instant.parse("2027-01-01T00:00:00Z")

private const val TEN = 10

/** Rather more than a year of daily snapshots — enough that an unbounded shelf is obvious. */
private const val MANY = 400

/** What one shelf — eligible, or future-stamped — can hold. The whole policy is two of these. */
private val SLOTS_PER_SHELF: Int =
    SnapshotRetention.DAILY_SLOTS + SnapshotRetention.WEEKLY_SLOTS + SnapshotRetention.MONTHLY_SLOTS

/** [count] consecutive calendar days ending at [endInclusive], oldest first. */
private fun consecutiveDaysTo(endInclusive: String, count: Int): List<RetentionCandidate> =
    snapshots(*stepBackFrom(endInclusive, count, DateTimeUnit.DAY).reversed().toTypedArray())

/** [count] dates one [unit] apart ending at [endInclusive], newest first. */
private fun stepBackFrom(endInclusive: String, count: Int, unit: DateTimeUnit.DateBased): List<String> =
    List(count) { LocalDate.parse(endInclusive).minus(it, unit).toString() }

/** Nine consecutive Mondays, newest first — one distinct ISO week each, spanning three months. */
private val MONDAYS = stepBackFrom("2026-08-10", SnapshotRetention.WEEKLY_SLOTS + 1, DateTimeUnit.WEEK)

/** Thirteen consecutive months, newest first — one distinct day, week and month each. */
private val FIFTEENTHS = stepBackFrom("2026-08-15", SnapshotRetention.MONTHLY_SLOTS + 1, DateTimeUnit.MONTH)
