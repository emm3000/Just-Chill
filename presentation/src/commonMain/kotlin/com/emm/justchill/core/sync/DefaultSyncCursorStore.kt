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
 */
class DefaultSyncCursorStore(private val prefs: AppPreferences) : SyncCursorStore {
    override fun lastPulledAt(userId: String): String? = prefs.lastPulledAt(userId)
    override fun setLastPulledAt(userId: String, cursor: String) = prefs.setLastPulledAt(userId, cursor)
    override fun clear(userId: String) = prefs.clearSyncMetadata(userId)
}
