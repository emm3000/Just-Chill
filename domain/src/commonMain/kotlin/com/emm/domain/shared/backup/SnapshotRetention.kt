package com.emm.domain.shared.backup

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.previousOrSame
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

data class RetentionCandidate(val id: String, val takenAtEpochMillis: Long)

data class RetentionDecision(val keep: List<String>, val delete: List<String>)

object SnapshotRetention {

    fun select(candidates: List<RetentionCandidate>, now: Instant, zone: TimeZone): RetentionDecision {
        val nowMillis: Long = now.toEpochMilliseconds()
        val (future, eligible) = candidates.partition { it.takenAtEpochMillis > nowMillis }

        val survivors: MutableSet<String> = mutableSetOf()
        survivors += eligible.newestPerSlot(zone)
        survivors += future.newestPerSlot(zone)

        val (keep, delete) = candidates.sortedWith(CHRONOLOGICAL).partition { it.id in survivors }
        return RetentionDecision(keep = keep.map { it.id }, delete = delete.map { it.id })
    }

    const val DAILY_SLOTS: Int = 7

    const val WEEKLY_SLOTS: Int = 8

    const val MONTHLY_SLOTS: Int = 12
}

private fun List<RetentionCandidate>.newestPerSlot(zone: TimeZone): List<String> =
    newestPerBucket(SnapshotRetention.DAILY_SLOTS, zone) { day -> day } +
        newestPerBucket(SnapshotRetention.WEEKLY_SLOTS, zone) { day -> day.previousOrSame(DayOfWeek.MONDAY) } +
        newestPerBucket(SnapshotRetention.MONTHLY_SLOTS, zone) { day -> LocalDate(day.year, day.month, 1) }

private fun List<RetentionCandidate>.newestPerBucket(
    slots: Int,
    zone: TimeZone,
    bucketOf: (LocalDate) -> LocalDate,
): List<String> = groupBy { bucketOf(it.dayIn(zone)) }
    .entries
    .sortedByDescending { it.key }
    .take(slots)
    .map { (_, inBucket) -> inBucket.maxWith(CHRONOLOGICAL).id }

private fun RetentionCandidate.dayIn(zone: TimeZone): LocalDate =
    Instant.fromEpochMilliseconds(takenAtEpochMillis).toLocalDateTime(zone).date

private val CHRONOLOGICAL: Comparator<RetentionCandidate> =
    compareBy({ it.takenAtEpochMillis }, { it.id })
