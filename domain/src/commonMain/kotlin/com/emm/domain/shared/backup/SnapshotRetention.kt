package com.emm.domain.shared.backup

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.previousOrSame
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** One stored snapshot, as the retention decision sees it: an opaque id and when it was taken. */
data class RetentionCandidate(val id: String, val takenAtEpochMillis: Long)

/**
 * Which candidates survive and which do not, both oldest first.
 *
 * The two lists partition the input exactly — every candidate appears in one and only one of them,
 * which is what makes "kept by the weekly rule, deleted by the daily one" inexpressible rather than
 * merely unlikely.
 */
data class RetentionDecision(val keep: List<String>, val delete: List<String>)

/**
 * How many snapshots survive, and which ones — ADR 009 Phase 2c's "7 daily + 8 weekly + 12 monthly".
 *
 * **It is pure, and that is the point of it being here rather than beside the bucket.** It knows no
 * object key, no prefix, no manifest and no network: ids in, ids out. This half is where an
 * off-by-one deletes somebody's financial history, and it is the only half that can be tested
 * exhaustively — which it is, on the host suite, with no storage anywhere in sight.
 *
 * ### Buckets are filled from the DATA, not anchored on the calendar
 *
 * A slot is one distinct calendar day / ISO week / month **that actually holds a snapshot**, newest
 * first — not "the last 7 days from today". The conventional grandfather-father-son reading anchors
 * the windows on the current date, and it has a failure mode this app cannot afford: a device whose
 * clock jumps forward, even once, would find every stored snapshot outside every window and delete
 * the lot. Anchoring buys nothing in exchange, either, because a device with a wrong clock also
 * stamps the names it writes with that wrong clock — the anchor and the data drift together.
 *
 * Filling from the data means retention advances when a **new snapshot arrives**, never merely
 * because time passed. A phone left in a drawer for a year comes back to every snapshot it had. The
 * storage bound is unchanged: at most `7 + 8 + 12` objects survive however long the app runs.
 *
 * ### Within a bucket the NEWEST survives
 *
 * Two snapshots on one calendar day occupy one daily slot, and the later one takes it. Ties on the
 * instant are broken by id so the answer never depends on the order the caller happened to list the
 * bucket in — a prune whose result shifts with listing order is a prune nobody can reproduce from a
 * bug report.
 *
 * ### The buckets are calendar concepts, so they are computed in [TimeZone]
 *
 * Snapshot **names** are stamped in UTC, because a name has to be unambiguous across devices. The
 * days, weeks and months here are resolved in the zone that is passed in, because the retention a
 * person perceives is in their own days: a snapshot taken at 23:30 in Lima belongs to that evening,
 * not to the following UTC morning. Do not "fix" this to UTC — the two questions are different and
 * `docs/DATE_AUDIT.md` rule #7 is what says the zone must arrive as an argument rather than be read
 * off the machine. Weeks start on Monday (ISO), months on the 1st.
 *
 * ### What it deliberately does not do
 *
 * It never verifies anything and never asks whether a snapshot is any good. A caller hands it only
 * snapshots that carry a verified sidecar; deciding what qualifies is the caller's job, and
 * falling back to the newest snapshot that VERIFIES is ADR 009 Phase 4's.
 */
object SnapshotRetention {

    /**
     * The surviving snapshots, and everything else.
     *
     * [now] is passed rather than read so the whole decision is a function of `(candidates, now,
     * zone)` and nothing else — no hidden clock, so a test can move time without moving a machine.
     * Its one job is the future: a candidate stamped **after** [now] is kept unconditionally and
     * fills no slot. Clock skew is the only thing that produces one, and the alternative — letting it
     * claim the newest slot in every bucket — would evict three real snapshots to make room for a
     * mistake. Keeping it costs one object.
     */
    fun select(candidates: List<RetentionCandidate>, now: Instant, zone: TimeZone): RetentionDecision {
        val nowMillis: Long = now.toEpochMilliseconds()
        val (future, eligible) = candidates.partition { it.takenAtEpochMillis > nowMillis }

        // A UNION of three independent selections, never three passes that can each delete. This is
        // what makes the grandfather-father-son bug — one bucket evicting what another is keeping —
        // structurally impossible instead of merely tested for.
        val survivors: MutableSet<String> = future.mapTo(mutableSetOf()) { it.id }
        survivors += eligible.newestPerBucket(DAILY_SLOTS, zone) { day -> day }
        survivors += eligible.newestPerBucket(WEEKLY_SLOTS, zone) { day -> day.previousOrSame(DayOfWeek.MONDAY) }
        survivors += eligible.newestPerBucket(MONTHLY_SLOTS, zone) { day -> LocalDate(day.year, day.month, 1) }

        val (keep, delete) = candidates.sortedWith(CHRONOLOGICAL).partition { it.id in survivors }
        return RetentionDecision(keep = keep.map { it.id }, delete = delete.map { it.id })
    }

    /** Seven distinct calendar days, not seven snapshots: a day that holds three still holds one slot. */
    const val DAILY_SLOTS: Int = 7

    /** Eight distinct ISO weeks — Monday to Sunday, in the caller's zone. */
    const val WEEKLY_SLOTS: Int = 8

    /** Twelve distinct calendar months. */
    const val MONTHLY_SLOTS: Int = 12
}

/**
 * The newest candidate of each of the [slots] most recent buckets, where [bucketOf] says what a
 * bucket is: the day itself, its Monday, or the first of its month.
 *
 * Grouping by a [LocalDate] rather than by a formatted key is deliberate — the three granularities
 * then order by the same `Comparable`, and "most recent bucket" needs no string that happens to sort
 * chronologically.
 */
private fun List<RetentionCandidate>.newestPerBucket(
    slots: Int,
    zone: TimeZone,
    bucketOf: (LocalDate) -> LocalDate,
): List<String> = groupBy { bucketOf(it.dayIn(zone)) }
    .entries
    .sortedByDescending { it.key }
    .take(slots)
    .map { (_, inBucket) -> inBucket.maxWith(CHRONOLOGICAL).id }

/** The calendar day this snapshot was taken on, as read by someone standing in [zone]. */
private fun RetentionCandidate.dayIn(zone: TimeZone): LocalDate =
    Instant.fromEpochMilliseconds(takenAtEpochMillis).toLocalDateTime(zone).date

/**
 * Oldest first; the greatest element is the newest.
 *
 * The id is the second key so the order is total. Two snapshots cannot share an id — it is an object
 * name — so no two candidates ever compare equal, and neither the survivor of a bucket nor the order
 * of the returned lists can depend on how the caller happened to list the bucket.
 */
private val CHRONOLOGICAL: Comparator<RetentionCandidate> =
    compareBy({ it.takenAtEpochMillis }, { it.id })
