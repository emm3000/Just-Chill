package com.emm.justchill.core.sync

import com.emm.domain.sync.SyncCursorStore
import platform.Foundation.NSUserDefaults

/**
 * iOS [SyncCursorStore] backed by [NSUserDefaults] — the platform analogue of Android's
 * [com.emm.justchill.core.preferences.AppPreferences]-backed AppPreferencesSyncCursorStore.
 *
 * Key scheme is identical to Android so the pull-cursor semantics (ADR 002) carry over unchanged:
 *  - `last_pulled_at_$userId` (String) — ISO-8601 UTC watermark of the last successful pull.
 *    Absent (nil) ⇒ null ⇒ first pull ⇒ full idempotent re-pull.
 *  - `last_synced_at_$userId` (Long) — managed by [IosSyncOrchestrator], NOT by this port (Android
 *    reads it via AppPreferences directly; the orchestrator does the iOS equivalent). This class only
 *    clears it here so account deletion wipes both keys together.
 */
internal class IosSyncCursorStore : SyncCursorStore {

    private val defaults: NSUserDefaults get() = NSUserDefaults.standardUserDefaults

    override fun lastPulledAt(userId: String): String? =
        defaults.stringForKey(lastPulledAtKey(userId))

    override fun setLastPulledAt(userId: String, cursor: String) {
        defaults.setObject(cursor, lastPulledAtKey(userId))
    }

    override fun clear(userId: String) {
        defaults.removeObjectForKey(lastPulledAtKey(userId))
        defaults.removeObjectForKey(lastSyncedAtKey(userId))
    }

    private fun lastPulledAtKey(userId: String) = "$KEY_LAST_PULLED_AT_PREFIX$userId"
    private fun lastSyncedAtKey(userId: String) = "$KEY_LAST_SYNCED_AT_PREFIX$userId"

    private companion object {
        const val KEY_LAST_PULLED_AT_PREFIX = "last_pulled_at_"
        const val KEY_LAST_SYNCED_AT_PREFIX = "last_synced_at_"
    }
}
