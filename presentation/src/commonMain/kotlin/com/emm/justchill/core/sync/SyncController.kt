package com.emm.justchill.core.sync

import kotlinx.coroutines.flow.Flow
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

    /**
     * One-shot events that require top-level UI handling (session expiry, manual-sync failure).
     *
     * Buffered until a collector attaches: the first sync cycle runs from `bootstrapAppGraph` in
     * Application.onCreate, long before any composition exists, and an event raised in that window
     * must still reach the user rather than being dropped on the floor.
     *
     * **Single-collector by contract.** Each event is delivered to exactly one collector — there is
     * exactly one, [com.emm.justchill.hh.shared.SyncEventsHandler] in the single shared nav host.
     * The backing channel cannot fan out, so a second collector would not duplicate events, it would
     * steal them. Anything else that needs to react to sync outcomes should observe [status].
     */
    val events: Flow<SyncEvent>

    /**
     * Offer a sync request. Overlapping calls collapse safely.
     *
     * @param manual when true, marks the cycle as manually requested so a failure surfaces to the
     *               user rather than being swallowed for silent retry.
     */
    fun requestSync(manual: Boolean = false)
}
