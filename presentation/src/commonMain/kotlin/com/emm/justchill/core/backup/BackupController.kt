package com.emm.justchill.core.backup

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Narrow read+trigger surface of the snapshot-backup pipeline that the shared UI consumes.
 *
 * The orchestration itself — lifecycle triggers, the session gate, the conflated request channel and
 * the export → upload → record → prune cycle — lives in [BackupOrchestrator], which implements this.
 * Shared UI depends only on this port: it observes [isBackingUp], collects [events] and asks for a
 * cycle. Deliberately mirrors `SyncController`, which it does **not** extend, share code with or sit
 * near: ADR 009 Phase 5 deletes every sync-named file, and this port has to outlive that.
 *
 * ### Why this exists now and did not exist in 2c-iii-b
 *
 * That unit declined to create it: one implementation, one method, zero consumers — the shape
 * `docs/CODE_QUALITY.md` marks "YAGNI, in, and it bites". 2c-iv is the condition it named for
 * reopening the question, because [requestBackup] alone cannot carry a manual tap. It is
 * fire-and-forget by construction (a `trySend` onto a conflated channel, returning `Unit`), so a
 * button wired straight to it reports success the instant the request is *queued* — for a backup
 * that has not started, may be sitting behind another cycle, and may still fail. The two members
 * above are what a tap gets answered with instead.
 */
interface BackupController {

    /**
     * True while the request consumer is processing one request, hot.
     *
     * It covers **every** cycle, automatic ones included, because it is a statement about the
     * pipeline rather than about the tap: a manual request arriving while an automatic cycle runs is
     * queued behind it, and a UI that claimed idle would be describing a device that is busy.
     */
    val isBackingUp: StateFlow<Boolean>

    /**
     * One-shot outcomes of **manually requested** cycles; see [BackupEvent].
     *
     * Backed by a shared flow with no replay, so an event raised with nobody listening is dropped.
     * That is the opposite choice from `SyncController.events`, which buffers into a channel, and
     * the requirement is genuinely opposite: sync's first cycle runs from `bootstrapAppGraph` before
     * any composition exists, so its events must wait for a collector. A backup event only exists
     * *because* a screen asked for one, and re-delivering it to whichever ProfileViewModel is built
     * next would put a stale snackbar on an unrelated visit.
     */
    val events: Flow<BackupEvent>

    /**
     * Ask for a backup. Overlapping calls collapse safely.
     *
     * @param manual when true the cycle skips the dirty check and the once-a-day cap, and its
     *   outcome is published to [events]. It still respects the session gate and the single-cycle
     *   guard — neither is a policy about when a backup is worth taking, and both are correctness.
     */
    fun requestBackup(manual: Boolean = false)
}
