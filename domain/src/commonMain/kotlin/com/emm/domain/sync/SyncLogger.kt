package com.emm.domain.sync

/**
 * Cross-layer diagnostics port. Named for its original sync-only scope, but no longer limited to
 * it: it also covers account deletion ([com.emm.domain.auth.DeleteUserAccountUseCase]) and the
 * claim-on-authentication observer ([com.emm.domain.auth.ClaimLocalDataOnAuthenticationUseCase]).
 *
 * Failures are deliberately swallowed in several places across those paths (silent-retry posture);
 * this port is what keeps those swallows observable. Implementations must never throw.
 */
interface SyncLogger {
    fun warn(message: String, throwable: Throwable? = null)
}
