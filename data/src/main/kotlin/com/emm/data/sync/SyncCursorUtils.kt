package com.emm.data.sync

import java.time.Instant
import java.time.OffsetDateTime

/**
 * Parses a server_updated_at string that may carry an offset (e.g. "2026-06-10T01:30:00.123456+00:00")
 * or a UTC 'Z' suffix. OffsetDateTime.parse handles both; toInstant() normalises to UTC.
 */
internal fun parseServerInstant(value: String): Instant = OffsetDateTime.parse(value).toInstant()

/**
 * Returns an overlap cursor shifted [OVERLAP_SECONDS] before [cursor] to cover the commit-ordering
 * race described in ADR 002. Safe to call with a null cursor — returns null to indicate
 * a full re-pull (no filter applied).
 *
 * Always emits a canonical 'Z'-terminated ISO-8601 string so PostgREST gte filters are stable
 * regardless of the original offset/fractional-second format received from the server.
 */
internal fun overlapCursor(cursor: String?): String? =
    cursor?.let { parseServerInstant(it).minusSeconds(OVERLAP_SECONDS).toString() }

private const val OVERLAP_SECONDS = 10L
