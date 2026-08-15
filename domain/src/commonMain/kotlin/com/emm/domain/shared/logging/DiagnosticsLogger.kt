package com.emm.domain.shared.logging

/**
 * Cross-layer diagnostics port: the one channel that keeps a deliberately swallowed failure visible.
 *
 * It was born under the sync package, named after it, and outgrew that name long before it moved —
 * it already covered account deletion ([com.emm.domain.auth.DeleteUserAccountUseCase]) and the
 * claim-on-authentication observer ([com.emm.domain.auth.ClaimLocalDataOnAuthenticationUseCase]),
 * neither of which is sync. What forced the move is ADR 009: `docs/sync/ADR009_PLAN.md` Phase 5
 * deletes every sync-named file, package and module outright, and the snapshot-backup pipeline
 * (2c-iii-b's `BackupOrchestrator`) needs this port to satisfy hard constraint 4 — no silent failure,
 * every failure path logging a distinct reason. A backup depending on a sync-named symbol would be a
 * survivor wired into something scheduled for deletion, so the name had to go with the package
 * rather than wait for Phase 5's "keep, renamed" row.
 *
 * Failures are deliberately swallowed in several places across those paths (silent-retry posture);
 * this port is what keeps those swallows observable. Implementations must never throw.
 */
interface DiagnosticsLogger {
    fun warn(message: String, throwable: Throwable? = null)
}
