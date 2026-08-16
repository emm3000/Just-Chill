package com.emm.domain.shared.backup

import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

// Exclusive: > BACKUP_STALE_AFTER_DAYS, not >=. A snapshot taken exactly 3 days ago is not yet stale.
const val BACKUP_STALE_AFTER_DAYS: Int = 3

data class BackupStaleness(val daysSinceLastBackup: Int, val isStale: Boolean)

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

        val days: Int = lastBackupDay.daysUntil(today).coerceAtLeast(0)

        val stale: Boolean = days > BACKUP_STALE_AFTER_DAYS &&
            hasLocalChangesSince(backupRepository.latestLocalChangeAt(), lastSuccessfulBackupAt)

        return BackupStaleness(daysSinceLastBackup = days, isStale = stale)
    }
}
