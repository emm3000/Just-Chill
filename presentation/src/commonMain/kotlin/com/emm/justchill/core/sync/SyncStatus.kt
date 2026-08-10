package com.emm.justchill.core.sync

import com.emm.domain.shared.error.DomainException

/**
 * Current sync status exposed to the UI.
 *
 * @property isSyncing true while a sync cycle is in progress.
 * @property lastSyncedAtMillis epoch-millis of the last successful sync for the current user,
 *           or null when the user has never synced or is not signed in.
 * @property lastSyncFailed true when the most recent sync cycle (manual or automatic) ended in
 *           a [DomainException]. Reset to false when a new cycle starts or completes successfully.
 *           Also reset on sign-out (mirrors [lastSyncedAtMillis] reset behaviour).
 */
data class SyncStatus(
    val isSyncing: Boolean = false,
    val lastSyncedAtMillis: Long? = null,
    val lastSyncFailed: Boolean = false,
)
