package com.emm.data.shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.char

/**
 * The one encoding of a transaction's `occurredAt` as text: `'2026-08-10T21:47:33'`.
 *
 * Storage and the backup file both use it, so a value written by either reads back identical.
 *
 * The seconds are always present, which `LocalDateTime.toString()` does not guarantee — it drops
 * them when they are zero. Both forms sort and compare correctly, but only a fixed width makes two
 * writes of the same moment produce the same bytes, and the schema comparison, the golden fixtures
 * and any future equality check all quietly depend on that.
 */
private val storageFormat = LocalDateTime.Format {
    date(LocalDate.Formats.ISO)
    char('T')
    hour()
    char(':')
    minute()
    char(':')
    second()
}

internal fun LocalDateTime.toOccurredAtText(): String = format(storageFormat)

/**
 * Reads a stored `occurredAt` back, or null when the text is not one.
 *
 * Null rather than a throw, and null rather than a guess: every caller already drops a row it
 * cannot interpret (see the mappers' skip-the-row policy), so an unreadable value makes one row
 * invisible instead of taking down a whole screen or a whole sync page.
 *
 * The parser is the lenient ISO one on purpose — it also accepts the seconds-less form that
 * `LocalDateTime.toString()` produces, so a value written before this format existed still reads.
 */
internal fun String.toOccurredAtOrNull(): LocalDateTime? = try {
    LocalDateTime.parse(this)
} catch (_: IllegalArgumentException) {
    null
}
