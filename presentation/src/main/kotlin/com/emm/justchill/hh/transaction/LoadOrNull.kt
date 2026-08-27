package com.emm.justchill.hh.transaction

import kotlinx.coroutines.CancellationException

// Intentional broad catch: a missing row beats a crashed screen. CancellationException must not be
// swallowed — a cancelled loader that survives it runs on and overwrites the newer call's result with
// its own: null at a direct-assignment site, an empty list behind `.orEmpty()`.
@Suppress("TooGenericExceptionCaught")
internal suspend fun <T> loadOrNull(block: suspend () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    null
}
