package com.emm.data.shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.char

/**
 * Seconds are always written, which LocalDateTime.toString() drops when they are zero. Only the
 * fixed width makes two writes of the same moment produce the same bytes, which the schema
 * comparison and the golden fixtures compare on.
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
 * The lenient ISO parser on purpose: it also accepts the seconds-less form LocalDateTime.toString()
 * produces, so values written before [storageFormat] existed still read back.
 */
internal fun String.toOccurredAtOrNull(): LocalDateTime? = try {
    LocalDateTime.parse(this)
} catch (_: IllegalArgumentException) {
    null
}
