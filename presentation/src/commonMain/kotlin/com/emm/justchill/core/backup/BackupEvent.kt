package com.emm.justchill.core.backup

import com.emm.domain.shared.error.DomainException

/**
 * How a **manually requested** backup cycle ended.
 *
 * Automatic cycles publish nothing here on purpose. They fire from the background/resume triggers,
 * so an event carrying one would surface a snackbar the user never asked for, minutes after they
 * left the screen — and the automatic pipeline's posture is already "the next trigger is the retry"
 * (`BackupOrchestrator.runBackup`). What a *tap* deserves is an answer, which is what this is for.
 * Same split, same reason, as `SyncEvent.SyncFailed`.
 *
 * ADR 009 Phase 3 adds backup *health* — last successful backup, staleness, consecutive failures —
 * and that is a state surface, not a one-shot one. It does not belong here.
 */
sealed interface BackupEvent {

    /** A snapshot was exported, uploaded, verified and recorded. */
    data object Succeeded : BackupEvent

    /**
     * The cycle ended without a recorded snapshot.
     *
     * [cause] is diagnostic, and **the UI is not expected to translate it**: every backup failure
     * reads the same to the user for now. See `ProfileViewModel.onBackupEvent` for why that is a
     * decision rather than laziness — the account-switch refusal arrives here as
     * [DomainException.Unauthorized], which `toUserMessage()` renders as "sesión expirada" and which
     * `SyncOrchestrator` answers by signing the user out. Neither is true of a backup failure.
     */
    data class Failed(val cause: DomainException) : BackupEvent
}
