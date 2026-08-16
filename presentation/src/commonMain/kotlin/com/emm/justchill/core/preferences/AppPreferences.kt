package com.emm.justchill.core.preferences

import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.domain.shared.backup.BackupFailureState
import com.russhwolf.settings.Settings

/**
 * Single commonMain key-value preferences facade over [Settings] (multiplatform-settings).
 *
 * Backed by SharedPreferences on Android (via SharedPreferencesSettings, wired in CoreModule) and
 * NSUserDefaults on iOS (via NSUserDefaultsSettings, wired in KoinIos). Keys, value types, and the
 * -1L "never synced" sentinel are preserved verbatim from the former Android-only implementation so
 * existing installs keep their onboarding state and sync cursor.
 */
class AppPreferences(private val settings: Settings) {
    var firstLaunchSeen: Boolean
        get() = settings.getBoolean(KEY_FIRST_LAUNCH_SEEN, false)
        set(value) = settings.putBoolean(KEY_FIRST_LAUNCH_SEEN, value)

    /**
     * Returns the last-pulled-at ISO-8601 UTC cursor for [userId], or null if the user has
     * never successfully pulled (triggers a full re-pull, which is safe and idempotent).
     */
    fun lastPulledAt(userId: String): String? = settings.getStringOrNull(userKey(KEY_LAST_PULLED_AT_PREFIX, userId))

    /**
     * Persists [cursor] as the new watermark for [userId].
     */
    fun setLastPulledAt(userId: String, cursor: String) {
        settings.putString(userKey(KEY_LAST_PULLED_AT_PREFIX, userId), cursor)
    }

    /**
     * Returns the epoch-millis timestamp of the last successful sync for [userId], or null if
     * the user has never completed a sync on this device (sentinel -1L = never).
     */
    fun lastSyncedAt(userId: String): Long? {
        val value = settings.getLong(userKey(KEY_LAST_SYNCED_AT_PREFIX, userId), -1L)
        return if (value == -1L) null else value
    }

    /**
     * Persists [epochMillis] as the last-synced-at timestamp for [userId].
     */
    fun setLastSyncedAt(userId: String, epochMillis: Long) {
        settings.putLong(userKey(KEY_LAST_SYNCED_AT_PREFIX, userId), epochMillis)
    }

    /**
     * Removes both per-user sync metadata keys for [userId]:
     * the pull cursor ([KEY_LAST_PULLED_AT_PREFIX]) and the last-synced-at timestamp
     * ([KEY_LAST_SYNCED_AT_PREFIX]). Called when the user deletes their account so stale
     * cursor data does not interfere if the same device registers again.
     */
    fun clearSyncMetadata(userId: String) {
        settings.remove(userKey(KEY_LAST_PULLED_AT_PREFIX, userId))
        settings.remove(userKey(KEY_LAST_SYNCED_AT_PREFIX, userId))
    }

    /**
     * Returns the epoch-millis timestamp of the last successful, verified backup upload for
     * [userId], or null if this device has never completed one (sentinel -1L = never).
     */
    fun lastSuccessfulBackupAt(userId: String): Long? {
        val value = settings.getLong(userKey(KEY_LAST_SUCCESSFUL_BACKUP_AT_PREFIX, userId), -1L)
        return if (value == -1L) null else value
    }

    /**
     * Persists [epochMillis] as the last-successful-backup-at timestamp for [userId].
     */
    fun setLastSuccessfulBackupAt(userId: String, epochMillis: Long) {
        settings.putLong(userKey(KEY_LAST_SUCCESSFUL_BACKUP_AT_PREFIX, userId), epochMillis)
    }

    /**
     * Removes the last-successful-backup-at key ([KEY_LAST_SUCCESSFUL_BACKUP_AT_PREFIX]) for
     * [userId].
     *
     * Deliberately a SEPARATE function from [clearSyncMetadata], not a third line added to it:
     * `clearSyncMetadata` and everything sync-named is deleted whole in ADR 009 Phase 5, and this
     * key must survive that deletion. Called through `BackupMetadataStore`'s implementation,
     * `DefaultBackupMetadataStore` (`:presentation`, `core/backup/`) — its own seam, not a side
     * effect of `DefaultSyncCursorStore.clear`. Failure this prevents: a user deletes their account
     * and registers again on the same device — with no clear, the stale timestamp claims a backup
     * already happened, the new account's bucket is empty, and the first snapshot for it never
     * fires.
     */
    fun clearBackupMetadata(userId: String) {
        settings.remove(userKey(KEY_LAST_SUCCESSFUL_BACKUP_AT_PREFIX, userId))
        settings.remove(userKey(KEY_BACKUP_FAILURE_PREFIX, userId))
    }

    /**
     * The persisted backup failure streak for [userId], or [BackupFailureState.None] when none is
     * recorded.
     *
     * **Both halves live under ONE key**, encoded `count|REASON_NAME`. Two keys is the obvious
     * spelling and it is wrong: `Settings` has no transaction, so two writes are two commits and a
     * process killed between them leaves a count with the previous outage's reason, or a reason with
     * no streak. One key makes the torn pair unrepresentable on disk rather than merely discouraged
     * in a KDoc — see [BackupFailureState], which used to claim exactly that guarantee while the
     * writer below did not provide it.
     *
     * Every malformed reading degrades to [BackupFailureState.None] instead of throwing: an absent
     * key, a value from a build that spelled this differently, a truncated string. The reason
     * resolves through [BackupFailureReason.fromNameOrNull], which answers null for a name this
     * build no longer has rather than throwing on a device that upgraded across a rename — so a
     * streak can legitimately survive with no reason attached to it.
     */
    fun backupFailure(userId: String): BackupFailureState {
        val stored: String = settings.getStringOrNull(userKey(KEY_BACKUP_FAILURE_PREFIX, userId)).orEmpty()
        return BackupFailureState(
            consecutiveFailures = stored.substringBefore(FAILURE_SEPARATOR).toIntOrNull() ?: 0,
            lastReason = BackupFailureReason.fromNameOrNull(stored.substringAfter(FAILURE_SEPARATOR, "")),
        )
    }

    /**
     * Persists [state] as the backup failure streak for [userId], in a single write.
     *
     * A null [BackupFailureState.lastReason] is encoded as an empty name rather than a sentinel word:
     * [backupFailure] already reads anything it cannot resolve as "no reason", so a placeholder would
     * be a second spelling of absence that only one of these two functions knows about.
     */
    fun setBackupFailure(userId: String, state: BackupFailureState) {
        settings.putString(
            userKey(KEY_BACKUP_FAILURE_PREFIX, userId),
            "${state.consecutiveFailures}$FAILURE_SEPARATOR${state.lastReason?.name.orEmpty()}",
        )
    }

    /**
     * The one spelling of the per-user key layout: a fixed prefix followed by the user id.
     *
     * It was three near-identical private functions until the backup failure streak arrived — the
     * same sentence written four times is repeated knowledge, and `TooManyFunctions` counts private
     * members, so four would also have pushed this class past detekt's per-class budget of 11.
     */
    private fun userKey(prefix: String, userId: String) = "$prefix$userId"

    private companion object {
        const val KEY_FIRST_LAUNCH_SEEN = "first_launch_seen"
        const val KEY_LAST_PULLED_AT_PREFIX = "last_pulled_at_"
        const val KEY_LAST_SYNCED_AT_PREFIX = "last_synced_at_"
        const val KEY_LAST_SUCCESSFUL_BACKUP_AT_PREFIX = "last_successful_backup_at_"
        const val KEY_BACKUP_FAILURE_PREFIX = "backup_failure_"

        /**
         * Splits the streak from the reason name inside the one value. Safe as a plain character
         * because the right-hand side is always an enum name, which cannot contain it.
         */
        const val FAILURE_SEPARATOR = '|'
    }
}
