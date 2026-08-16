package com.emm.justchill.core.backup

import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.domain.shared.backup.BackupFailureState
import com.emm.domain.shared.backup.BackupMetadataStore
import com.russhwolf.settings.Settings

// Key strings and the -1L "never" sentinel are load-bearing: an install already holds values under
// them, and renaming one reads as "this device never backed up".
class DefaultBackupMetadataStore(private val settings: Settings) : BackupMetadataStore {

    override fun lastSuccessfulBackupAt(userId: String): Long? {
        val value: Long = settings.getLong(userKey(KEY_LAST_SUCCESSFUL_BACKUP_AT_PREFIX, userId), NEVER)
        return if (value == NEVER) null else value
    }

    override fun setLastSuccessfulBackupAt(userId: String, epochMillis: Long) {
        settings.putLong(userKey(KEY_LAST_SUCCESSFUL_BACKUP_AT_PREFIX, userId), epochMillis)
    }

    // The count gates the whole reading: parse the halves independently and `abc|Network` yields
    // (0, Network), the disagreeing pair the single key exists to make unrepresentable. Nothing
    // throws on the way — this is the read that runs while the app is reporting a failure.
    override fun failureState(userId: String): BackupFailureState {
        val stored: String = settings.getStringOrNull(userKey(KEY_BACKUP_FAILURE_PREFIX, userId)).orEmpty()
        val count: Int = stored.substringBefore(FAILURE_SEPARATOR).toIntOrNull() ?: return BackupFailureState.None
        val reasonName: String = stored.substringAfter(FAILURE_SEPARATOR, "")
        return BackupFailureState(count, BackupFailureReason.fromNameOrNull(reasonName))
    }

    override fun recordFailure(userId: String, reason: BackupFailureReason): BackupFailureState {
        val next = BackupFailureState(
            consecutiveFailures = failureState(userId).consecutiveFailures + 1,
            lastReason = reason,
        )
        writeFailure(userId, next)
        return next
    }

    override fun clearFailures(userId: String) = writeFailure(userId, BackupFailureState.None)

    override fun destinationDisclosedAt(userId: String): Long? {
        val value: Long = settings.getLong(userKey(KEY_DESTINATION_DISCLOSED_AT_PREFIX, userId), NEVER)
        return if (value == NEVER) null else value
    }

    override fun setDestinationDisclosed(userId: String, epochMillis: Long) {
        settings.putLong(userKey(KEY_DESTINATION_DISCLOSED_AT_PREFIX, userId), epochMillis)
    }

    override fun clear(userId: String) {
        settings.remove(userKey(KEY_LAST_SUCCESSFUL_BACKUP_AT_PREFIX, userId))
        settings.remove(userKey(KEY_BACKUP_FAILURE_PREFIX, userId))
        settings.remove(userKey(KEY_DESTINATION_DISCLOSED_AT_PREFIX, userId))
    }

    // Both halves of the streak go under ONE key, `count|REASON_NAME`: Settings has no transaction,
    // so two keys are two commits and a process killed between them leaves a count carrying the
    // previous outage's reason.
    private fun writeFailure(userId: String, state: BackupFailureState) {
        settings.putString(
            userKey(KEY_BACKUP_FAILURE_PREFIX, userId),
            "${state.consecutiveFailures}$FAILURE_SEPARATOR${state.lastReason?.name.orEmpty()}",
        )
    }

    private companion object {
        const val NEVER = -1L
        const val KEY_LAST_SUCCESSFUL_BACKUP_AT_PREFIX = "last_successful_backup_at_"
        const val KEY_BACKUP_FAILURE_PREFIX = "backup_failure_"
        const val KEY_DESTINATION_DISCLOSED_AT_PREFIX = "backup_destination_disclosed_at_"
        const val FAILURE_SEPARATOR = '|'
    }
}

private fun userKey(prefix: String, userId: String) = "$prefix$userId"
