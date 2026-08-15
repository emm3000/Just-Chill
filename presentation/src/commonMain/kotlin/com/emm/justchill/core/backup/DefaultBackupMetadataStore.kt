package com.emm.justchill.core.backup

import com.emm.domain.shared.backup.BackupMetadataStore
import com.emm.justchill.core.preferences.AppPreferences

/**
 * Single commonMain [BackupMetadataStore] backed by [AppPreferences].
 *
 * Mirrors `DefaultSyncCursorStore`'s shape (`core/sync/`) but deliberately lives outside that
 * package: this seam is not sync metadata, and unlike `DefaultSyncCursorStore` it is NOT deleted by
 * ADR 009 Phase 5 — the backup watermark and its clear are the pipeline this whole plan is building
 * toward, not the engine Phase 5 removes.
 */
class DefaultBackupMetadataStore(private val prefs: AppPreferences) : BackupMetadataStore {
    override fun lastSuccessfulBackupAt(userId: String): Long? = prefs.lastSuccessfulBackupAt(userId)

    override fun setLastSuccessfulBackupAt(userId: String, epochMillis: Long) =
        prefs.setLastSuccessfulBackupAt(userId, epochMillis)

    override fun clear(userId: String) = prefs.clearBackupMetadata(userId)
}
