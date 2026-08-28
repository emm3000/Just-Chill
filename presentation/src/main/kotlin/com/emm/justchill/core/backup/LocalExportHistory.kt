package com.emm.justchill.core.backup

import com.russhwolf.settings.Settings
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

// The file the user saves themselves, never the cloud snapshot pipeline: no userId scopes the key,
// because an exported file leaves the device whoever is signed in, and every
// DefaultBackupMetadataStore key is per-user by construction.
class LocalExportHistory(private val settings: Settings, private val clock: Clock, private val timeZone: TimeZone) {

    fun daysSinceLastExport(): Int? {
        val exportedAt: Long = settings.getLong(KEY_LAST_EXPORT_AT, NEVER)
        if (exportedAt == NEVER) return null
        val exportDay = Instant.fromEpochMilliseconds(exportedAt).toLocalDateTime(timeZone).date
        return exportDay.daysUntil(clock.now().toLocalDateTime(timeZone).date).coerceAtLeast(0)
    }

    fun recordExport() = settings.putLong(KEY_LAST_EXPORT_AT, clock.now().toEpochMilliseconds())

    private companion object {
        const val NEVER = -1L
        const val KEY_LAST_EXPORT_AT = "last_local_export_at"
    }
}
