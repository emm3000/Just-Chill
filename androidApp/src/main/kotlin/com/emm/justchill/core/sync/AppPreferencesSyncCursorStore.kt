package com.emm.justchill.core.sync

import com.emm.domain.sync.SyncCursorStore
import com.emm.justchill.core.preferences.AppPreferences

/**
 * [SyncCursorStore] port implementation backed by [AppPreferences] / SharedPreferences.
 *
 * Lives in :app so it can access AppPreferences without violating the :data → :domain boundary
 * (:data cannot depend on :app).
 */
class AppPreferencesSyncCursorStore(private val prefs: AppPreferences) : SyncCursorStore {
    override fun lastPulledAt(userId: String): String? = prefs.lastPulledAt(userId)
    override fun setLastPulledAt(userId: String, cursor: String) = prefs.setLastPulledAt(userId, cursor)
    override fun clear(userId: String) = prefs.clearSyncMetadata(userId)
}
