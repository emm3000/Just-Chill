package com.emm.data.shared

import kotlin.time.Clock

/**
 * Epoch millis from an injected [Clock] — the storage layer's stamp for `createdAt`, `updatedAt`
 * and `deletedAt`.
 *
 * The point is not the conversion, it is the receiver. This used to be `currentTimeInMillis()`, a
 * top-level function reading `Clock.System` wherever it was called, so no test could pin the
 * timestamps a write produced and nothing could vary them.
 *
 * Read it ONCE per write and reuse the value. Two calls in one statement are two different
 * instants: `createdAt` and `updatedAt` on a fresh row used to disagree by a millisecond, which is
 * a row that looks edited the moment it is born.
 */
internal fun Clock.nowMillis(): Long = now().toEpochMilliseconds()
