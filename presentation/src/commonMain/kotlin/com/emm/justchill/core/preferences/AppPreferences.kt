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
        settings.remove(userKey(KEY_BACKUP_FAILURE_COUNT_PREFIX, userId))
        settings.remove(userKey(KEY_BACKUP_FAILURE_REASON_PREFIX, userId))
    }

    /**
     * The persisted backup failure streak for [userId], or [BackupFailureState.None] when none is
     * recorded.
     *
     * Returns the domain value rather than the two primitives behind it because the two keys are one
     * fact and are always read and written together — argued on [BackupFailureState]. The reason is
     * stored by enum NAME and resolved through [BackupFailureReason.fromNameOrNull], which answers
     * null for a spelling this build no longer has instead of throwing on a device that upgraded
     * across a rename.
     */
    fun backupFailure(userId: String): BackupFailureState = BackupFailureState(
        consecutiveFailures = settings.getInt(userKey(KEY_BACKUP_FAILURE_COUNT_PREFIX, userId), 0),
        lastReason = BackupFailureReason.fromNameOrNull(
            settings.getStringOrNull(userKey(KEY_BACKUP_FAILURE_REASON_PREFIX, userId)),
        ),
    )

    /**
     * Persists [state] as the backup failure streak for [userId].
     *
     * A null [BackupFailureState.lastReason] REMOVES the reason key rather than writing a sentinel:
     * absence is already how [backupFailure] reads "no reason", and a written placeholder would be a
     * second spelling of it that only one of the two functions knows about.
     */
    fun setBackupFailure(userId: String, state: BackupFailureState) {
        settings.putInt(userKey(KEY_BACKUP_FAILURE_COUNT_PREFIX, userId), state.consecutiveFailures)
        val reasonKey = userKey(KEY_BACKUP_FAILURE_REASON_PREFIX, userId)
        val reason = state.lastReason
        if (reason == null) settings.remove(reasonKey) else settings.putString(reasonKey, reason.name)
    }

    /**
     * The one spelling of the per-user key layout: a fixed prefix followed by the user id.
     *
     * It was three near-identical private functions until the backup failure streak needed two more
     * keys — the same sentence written five times is repeated knowledge, and `TooManyFunctions`
     * counts private members, so five would also have pushed this class past detekt's per-class
     * budget of 11.
     */
    private fun userKey(prefix: String, userId: String) = "$prefix$userId"

    private companion object {
        const val KEY_FIRST_LAUNCH_SEEN = "first_launch_seen"
        const val KEY_LAST_PULLED_AT_PREFIX = "last_pulled_at_"
        const val KEY_LAST_SYNCED_AT_PREFIX = "last_synced_at_"
        const val KEY_LAST_SUCCESSFUL_BACKUP_AT_PREFIX = "last_successful_backup_at_"
        const val KEY_BACKUP_FAILURE_COUNT_PREFIX = "backup_failure_count_"
        const val KEY_BACKUP_FAILURE_REASON_PREFIX = "backup_failure_reason_"
    }
}
