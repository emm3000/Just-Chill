package com.emm.data.sync

import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds

internal fun parseServerInstant(value: String): Instant = Instant.parse(value)

internal fun overlapCursor(cursor: String?): String? =
    cursor?.let { (parseServerInstant(it) - OVERLAP_SECONDS.seconds).toString() }

private const val OVERLAP_SECONDS = 10L
