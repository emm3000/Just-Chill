package com.emm.justchill.core.sync

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Narrow read+trigger surface of the sync engine that the shared UI consumes.
 *
 * The full orchestration machinery (lifecycle triggers, debounced writes, key-value-backed cursors,
 * one-shot [SyncEvent]s) lives in the commonMain [SyncOrchestrator], shared by both platforms, which
 * implements this interface. commonMain code depends only on this port — it observes [status],
 * requests cycles, and collects [events].
 */
interface SyncController {

    /** Current sync status, hot. */
    val status: StateFlow<SyncStatus>

    /** One-shot events that require top-level UI handling (session expiry, manual-sync failure). */
    val events: SharedFlow<SyncEvent>

    /**
     * Offer a sync request. Overlapping calls collapse safely.
     *
     * @param manual when true, marks the cycle as manually requested so a failure surfaces to the
     *               user rather than being swallowed for silent retry.
     */
    fun requestSync(manual: Boolean = false)
}
