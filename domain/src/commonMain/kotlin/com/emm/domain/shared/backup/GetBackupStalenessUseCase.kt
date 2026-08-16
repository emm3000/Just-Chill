package com.emm.domain.shared.backup

import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * How many calendar days a verified snapshot may be older than before the device is called stale.
 *
 * `docs/sync/ADR009_PLAN.md` Phase 3 states the rule as "warning state when staleness exceeds 3 days
 * with pending mutations", and *exceeds* is why the comparison below is `>` and not `>=`: a snapshot
 * taken three days ago is exactly at the threshold, not past it.
 *
 * A constant rather than a constructor parameter. There is one product rule, one caller and no
 * second policy to configure — `docs/CODE_QUALITY.md` marks that shape "YAGNI, in, and it bites" —
 * and a named constant is still the thing a test asserts the boundary against.
 */
const val BACKUP_STALE_AFTER_DAYS: Int = 3

/**
 * How out of date this device's last verified snapshot is.
 *
 * Both fields come from **one** clock read, which is the reason they travel together instead of
 * being asked for separately: a caller that read the age and then asked whether it was stale could
 * straddle midnight between the two questions and render "hace 3 días" beside a stale warning that
 * disagrees with it.
 *
 * @property daysSinceLastBackup whole calendar days between the last verified snapshot's day and
 *   today, in the injected zone. `0` is "today", never negative — see [GetBackupStalenessUseCase].
 * @property isStale both halves of the rule held: the snapshot is older than [BACKUP_STALE_AFTER_DAYS]
 *   **and** there is local data it does not hold.
 */
data class BackupStaleness(val daysSinceLastBackup: Int, val isStale: Boolean)

/**
 * Is this device's last verified snapshot old enough, and its ledger changed enough, to warn about?
 *
 * ### Why this is a use case and not a ViewModel `if`
 *
 * `docs/CODE_QUALITY.md`'s admission rule lets a pure read go straight from a ViewModel to a
 * repository and admits a use case only where there is domain logic. This is the second: the answer
 * is a conjunction of two independent predicates — an age measured in calendar days against a
 * product threshold, and a comparison of the local write watermark against the backup watermark —
 * over data the caller does not have (`latestLocalChangeAt`) and a clock and zone the caller must
 * not read for itself. It is not `repository.x(...)` under a new name; deleting it would push a
 * calendar-day computation and a product constant into `:presentation`, where iOS would have to
 * grow its own copy.
 *
 * ### Both halves are required, and the order is load-bearing
 *
 * A device with nothing new to back up is not stale however long ago its last snapshot was — a
 * ledger nobody has touched in a month is completely backed up, and warning about it would be an
 * alarm with no action behind it. The age half is evaluated first so the database read only happens
 * once the age already justifies it; `&&` is what keeps that order real rather than merely written.
 *
 * ### Calendar days, not `now - 3 * 24h`
 *
 * `docs/DATE_AUDIT.md` #8 is the finding that a "last N days" window measured in fixed milliseconds
 * is a different question from one measured in days — it drifts by an hour across a DST change and
 * otherwise starts mid-morning. So the age here is the difference between two *local dates*, which
 * is exactly why [timeZone] is a real dependency and not decoration. Neither [clock] nor [timeZone]
 * carries a default, per that document's live rule 7: a parameter defaulting to the ambient zone
 * lets a caller read the machine without saying so, and `hh/di/SharedModule.kt` is the only way one
 * enters the graph.
 *
 * ### `lastSuccessfulBackupAt` is non-null on purpose
 *
 * A device that has never completed a snapshot is a **louder** statement than a stale one, and the
 * caller has to make it anyway — so it is not expressed here as a `null` this class would have to
 * answer with an arbitrary `true` or `false` that nothing could ever observe. The type is what
 * forces the caller to resolve that case before asking this question.
 */
class GetBackupStalenessUseCase(
    private val backupRepository: BackupRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {

    suspend operator fun invoke(lastSuccessfulBackupAt: Long): BackupStaleness {
        val now: Instant = clock.now()
        val today = now.toLocalDateTime(timeZone).date
        val lastBackupDay = Instant.fromEpochMilliseconds(lastSuccessfulBackupAt)
            .toLocalDateTime(timeZone).date

        // Floored at zero: a watermark in the future is a device whose clock moved backwards (or a
        // snapshot recorded by a phone set ahead), and "hace -2 días" is a sentence no screen should
        // ever be asked to render. It reads as "today", which is the honest of the two answers.
        val days: Int = lastBackupDay.daysUntil(today).coerceAtLeast(0)

        val stale: Boolean = days > BACKUP_STALE_AFTER_DAYS &&
            hasLocalChangesSince(backupRepository.latestLocalChangeAt(), lastSuccessfulBackupAt)

        return BackupStaleness(daysSinceLastBackup = days, isStale = stale)
    }
}
