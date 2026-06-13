package com.emm.justchill.core.sync

import kotlinx.coroutines.flow.StateFlow

/**
 * Narrow read+trigger surface of the sync engine that the shared UI consumes.
 *
 * The full orchestration machinery (lifecycle triggers, connectivity, SharedPreferences-backed
 * cursors, one-shot [SyncEvent]s) is Android-coupled and lives in `:app`'s `SyncOrchestrator`,
 * which implements this interface. commonMain code depends only on this port — it observes
 * [status] and requests cycles, nothing more.
 */
interface SyncController {

    /** Current sync status, hot. */
    val status: StateFlow<SyncStatus>

    /**
     * Offer a sync request. Overlapping calls collapse safely.
     *
     * @param manual when true, marks the cycle as manually requested so a failure surfaces to the
     *               user rather than being swallowed for silent retry.
     */
    fun requestSync(manual: Boolean = false)
}
