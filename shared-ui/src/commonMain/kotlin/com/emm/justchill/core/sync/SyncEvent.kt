package com.emm.justchill.core.sync

import com.emm.domain.shared.error.DomainException

/** One-shot events emitted by [SyncOrchestrator] that require top-level UI handling. */
sealed interface SyncEvent {
    /** The remote session was revoked — the user has been signed out automatically. */
    data object SessionExpired : SyncEvent

    /** A manually-requested sync cycle failed (non-[DomainException.Unauthorized]). */
    data class SyncFailed(val error: DomainException) : SyncEvent
}
