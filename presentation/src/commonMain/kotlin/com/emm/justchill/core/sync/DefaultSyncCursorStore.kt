package com.emm.justchill.core.sync

import com.emm.domain.sync.SyncCursorStore
import com.emm.justchill.core.preferences.AppPreferences

/**
 * Single commonMain [SyncCursorStore] backed by [AppPreferences].
 *
 * Replaces the former platform-split AppPreferencesSyncCursorStore (Android, SharedPreferences) and
 * IosSyncCursorStore (iOS, NSUserDefaults), which carried identical key schemes. Persistence is now
 * delegated to [AppPreferences], whose multiplatform-settings backend is SharedPreferences on Android
 * and NSUserDefaults on iOS — so this single implementation works on both platforms. [clear] wipes
 * both the pull cursor and the last-synced-at timestamp via [AppPreferences.clearSyncMetadata].
 *
 * It also clears the persisted last-successful-backup timestamp via
 * [AppPreferences.clearBackupMetadata], riding the same account-deletion call
 * (`DeleteUserAccountUseCase`'s "cursor clear" step) rather than getting a call site of its own.
 * That is a temporary home, not a design decision: this whole class is sync-named and is deleted
 * in ADR 009 Phase 5, but `clearBackupMetadata` must NOT go with it — when Phase 5 removes
 * [SyncCursorStore] and this class, move the `clearBackupMetadata` call to whatever step still
 * runs on account deletion afterwards.
 */
class DefaultSyncCursorStore(private val prefs: AppPreferences) : SyncCursorStore {
    override fun lastPulledAt(userId: String): String? = prefs.lastPulledAt(userId)
    override fun setLastPulledAt(userId: String, cursor: String) = prefs.setLastPulledAt(userId, cursor)

    override fun clear(userId: String) {
        prefs.clearSyncMetadata(userId)
        prefs.clearBackupMetadata(userId)
    }
}
