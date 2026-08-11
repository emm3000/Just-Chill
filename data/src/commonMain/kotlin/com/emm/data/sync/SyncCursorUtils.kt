package com.emm.data.sync

import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Parses a `server_updated_at` value, or null when it is not a timestamp this client understands.
 *
 * Nullable because the string is remote input like any other column. [BaseTableSync] already guards
 * the case where the server sends no value at all; a value it cannot read is the same kind of
 * problem and must not be the one that throws — the parse happens inside the per-page database
 * transaction, so an exception there takes down the whole pull rather than one row.
 */
internal fun parseServerInstant(value: String): Instant? = runCatching { Instant.parse(value) }.getOrNull()

/**
 * The stored cursor, rewound by [OVERLAP_SECONDS] so a pull re-reads the rows around where the last
 * one stopped. Null when there is no cursor, or when the stored one cannot be parsed — both mean
 * "start from the beginning", which is the self-healing answer: a full re-pull is idempotent, and a
 * cursor the client cannot read would otherwise wedge sync permanently.
 */
internal fun overlapCursor(cursor: String?): String? =
    cursor?.let(::parseServerInstant)?.let { (it - OVERLAP_SECONDS.seconds).toString() }

private const val OVERLAP_SECONDS = 10L
