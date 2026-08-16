package com.emm.data.backup

import kotlin.time.Instant

fun backupSnapshotName(takenAt: Instant): String {
    val stamp = Instant.fromEpochSeconds(takenAt.epochSeconds).toString()
    return "$SNAPSHOT_PREFIX${stamp.replace(':', TIME_SEPARATOR)}$JSON_EXTENSION"
}

// Well-formed digits in the right places, not a real instant, is the same answer as a name that
// never looked like a snapshot: not a snapshot, so left alone.
@Suppress("SwallowedException")
internal fun parseBackupSnapshotTakenAt(fileName: String): Instant? {
    val groups: List<String> = SNAPSHOT_NAME.matchEntire(fileName)?.groupValues ?: return null
    return try {
        Instant.parse("${groups[DATE]}T${groups[HOURS]}:${groups[MINUTES]}:${groups[SECONDS]}Z")
    } catch (ignored: IllegalArgumentException) {
        null
    }
}

internal fun manifestNameFor(fileName: String): String = "$fileName$BACKUP_MANIFEST_SUFFIX"

internal const val BACKUP_MANIFEST_SUFFIX: String = ".manifest.json"

private const val JSON_EXTENSION: String = ".json"

private const val SNAPSHOT_PREFIX: String = "backup-v$BACKUP_SCHEMA_VERSION-"

private const val TIME_SEPARATOR: Char = '-'

private const val DATE = 1
private const val HOURS = 2
private const val MINUTES = 3
private const val SECONDS = 4

private val SNAPSHOT_NAME = Regex(
    SNAPSHOT_PREFIX + """(\d{4}-\d{2}-\d{2})T(\d{2})-(\d{2})-(\d{2})Z\.json""",
)
