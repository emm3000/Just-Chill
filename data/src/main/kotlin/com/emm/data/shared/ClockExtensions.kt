package com.emm.data.shared

import kotlin.time.Clock

/**
 * Read once per write and reuse the value: two calls are two instants, and a fresh row whose
 * createdAt and updatedAt differ by a millisecond looks edited the moment it is born.
 */
internal fun Clock.nowMillis(): Long = now().toEpochMilliseconds()
