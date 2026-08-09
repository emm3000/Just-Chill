package com.emm.domain.sync

/**
 * Cross-layer logging port for the sync path.
 *
 * Sync failures are deliberately swallowed in several places (silent-retry posture); this port is
 * what keeps those swallows observable. Implementations must never throw.
 */
interface SyncLogger {
    fun warn(message: String, throwable: Throwable? = null)
}
