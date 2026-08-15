package com.emm.data.backup

import kotlin.time.Instant

/**
 * The name for a snapshot taken at [takenAt] — UTC, truncated to the whole second.
 *
 * ADR 009 Phase 2c's Naming row: `backup-v<schema>-<ISO 8601 UTC, colons replaced>.json`. This
 * builder and [parseBackupSnapshotTakenAt] live in one file **on purpose**. The name is the only
 * place a snapshot's timestamp survives — the retention prune has nothing else to sort on, since a
 * listing carries names and nothing else this app wrote — so a generator and a reader that disagree
 * by one character do not fail loudly: the prune simply stops recognising its own snapshots. Written
 * together, a round-trip test can say they agree; written in two units, nothing can.
 *
 * **UTC in the name, the user's zone in the buckets.** The stamp is UTC because a name has to be
 * unambiguous and comparable across devices and zones; the daily/weekly/monthly slots the prune fills
 * are calendar concepts and are computed in the injected `TimeZone` instead. Those are two different
 * questions and it is not an inconsistency to answer them differently — see `SnapshotRetention`.
 *
 * **Second precision, and the truncation is load-bearing.** `Instant.toString()` prints sub-second
 * digits when it has them, so a payload taken at `…:00.123Z` and one taken at `…:00Z` would produce
 * names of different lengths whose lexicographic order disagrees with their chronological order
 * (`.` sorts before `Z`). Everything downstream of a listing is easier if one snapshot means one
 * fixed-width name, and one second of resolution is far finer than the one-snapshot-per-day cap the
 * Trigger row sets.
 *
 * Valid for the four-digit years [parseBackupSnapshotTakenAt] accepts, which is every year this app
 * will run in; outside them the stamp gains a sign and an extra digit and the pair stops
 * round-tripping. `every name the builder writes is a name the parser reads back` is what holds the
 * two ends together, and it is the test to break if this format is ever changed.
 *
 * **It is the one half of this file that is public, and the visibility is load-bearing.** ADR 009
 * Phase 2c-iii orchestrates the snapshot from `:presentation`, which is where `BackupUploader.upload`
 * is called and therefore where the name is chosen. Left `internal`, that unit had two options and
 * both were bad: widen this in a hurry, or spell the format out a second time — and a second
 * generator that disagrees with [parseBackupSnapshotTakenAt] by one character is exactly the failure
 * the builder-and-parser-in-one-file decision exists to prevent, since the prune would simply stop
 * recognising this app's own snapshots. Nothing else here follows: reading a name back is the prune's
 * business, and the prune lives in `:data`.
 */
fun backupSnapshotName(takenAt: Instant): String {
    val stamp = Instant.fromEpochSeconds(takenAt.epochSeconds).toString()
    return "$SNAPSHOT_PREFIX${stamp.replace(':', TIME_SEPARATOR)}$JSON_EXTENSION"
}

/**
 * The instant [fileName] was taken at, or `null` if it is not a snapshot name this build writes.
 *
 * **Total and defensive, and never throwing is the whole contract.** This runs over a bucket
 * listing, which holds whatever anyone ever put there: sidecars, folders, a hand-uploaded file, a
 * name from a future format. Every one of those is *not a snapshot*, and a non-snapshot is not a
 * prune candidate — it is left alone. Throwing would abort a prune over a stray object; returning a
 * best guess would hand a retention slot to something that is not a snapshot, and the object it
 * evicted to make room would be one that was verified.
 *
 * **It only recognises the CURRENT schema version**, which is why the prefix interpolates
 * [BACKUP_SCHEMA_VERSION] rather than spelling a 3. The consequence is deliberate: the day the
 * payload format moves to 4, every v3 snapshot already in the bucket stops parsing, so the prune
 * stops seeing them and therefore never deletes them. That costs storage and cannot cost data, which
 * is the right way round for this unit. Whoever bumps the version owns deciding whether the previous
 * generation should be swept, and has a migration-shaped place to say so.
 *
 * The shape is checked before [Instant.parse] rather than after, because `parse` is lenient in ways a
 * name must not be — a match here means fixed-width digits in fixed positions, and the parse that
 * follows only has to rule out the values that are well-formed but not real dates (`2026-13-40`).
 */
@Suppress("SwallowedException")
internal fun parseBackupSnapshotTakenAt(fileName: String): Instant? {
    val groups: List<String> = SNAPSHOT_NAME.matchEntire(fileName)?.groupValues ?: return null
    return try {
        Instant.parse("${groups[DATE]}T${groups[HOURS]}:${groups[MINUTES]}:${groups[SECONDS]}Z")
    } catch (ignored: IllegalArgumentException) {
        // Well-formed digits in the right places, not a real instant. Same answer as a name that
        // never looked like a snapshot: not a snapshot, so not a candidate, so left alone.
        null
    }
}

/**
 * The sidecar's name: the payload's, plus a suffix.
 *
 * **It appends rather than replacing the extension**, which looks clumsier and is the point. The
 * bucket only accepts `application/json`, and storage-kt derives that header from the key's
 * extension, so a name that stops ending in `.json` is refused with an HTTP 415 that reads like a
 * server fault. Appending cannot produce one whatever the caller passed; swapping an extension
 * quietly can, for any file name that did not have the extension the rule assumed.
 *
 * It also gives the pair a shape a listing can read without downloading anything: [BACKUP_MANIFEST_SUFFIX]
 * is the manifest, everything else is a payload, and no payload name can collide with a sidecar —
 * [SNAPSHOT_NAME] is anchored, so a sidecar never parses as the snapshot it describes.
 */
internal fun manifestNameFor(fileName: String): String = "$fileName$BACKUP_MANIFEST_SUFFIX"

/**
 * What makes a name a sidecar, for the one reader that has to recognise one it did not write.
 *
 * It is a constant rather than a literal repeated in two files because the prune derives a payload
 * name by removing it: a suffix that drifted from what [manifestNameFor] appends would make every
 * sidecar look like an orphan, and orphans are deleted on sight.
 */
internal const val BACKUP_MANIFEST_SUFFIX: String = ".manifest.json"

private const val JSON_EXTENSION: String = ".json"

private const val SNAPSHOT_PREFIX: String = "backup-v$BACKUP_SCHEMA_VERSION-"

/** The character a colon becomes. Object keys tolerate `:`, but a downloaded file name does not. */
private const val TIME_SEPARATOR: Char = '-'

/**
 * `backup-v3-YYYY-MM-DDTHH-MM-SSZ.json`, anchored by [Regex.matchEntire] at every call site.
 *
 * The date's own hyphens and the two that replaced colons are told apart positionally rather than by
 * a blind `replace`, which would turn `2026-08-14` into `2026:08:14` on the way back.
 */
// Capture-group indices into [SNAPSHOT_NAME]; group 0 is the whole match.
private const val DATE = 1
private const val HOURS = 2
private const val MINUTES = 3
private const val SECONDS = 4

private val SNAPSHOT_NAME = Regex(
    // [SNAPSHOT_PREFIX] is interpolated raw because it holds no regex metacharacter — it is letters,
    // a digit and hyphens. The extension is written out with its dot escaped rather than
    // interpolated, since that one IS a metacharacter and an unescaped `.` would match any byte.
    SNAPSHOT_PREFIX + """(\d{4}-\d{2}-\d{2})T(\d{2})-(\d{2})-(\d{2})Z\.json""",
)
