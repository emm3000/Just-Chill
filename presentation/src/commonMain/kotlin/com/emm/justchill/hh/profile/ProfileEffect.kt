package com.emm.justchill.hh.profile

import com.emm.domain.shared.error.DomainException
import com.emm.justchill.core.mvi.UiEffect

sealed interface ProfileEffect : UiEffect {
    data class ShowError(val error: DomainException) : ProfileEffect
    data class Notify(val message: ProfileMessage) : ProfileEffect

    /**
     * The backup JSON is ready. The platform layer (nav host) owns the SAF file IO:
     * it picks a destination and writes [json] to it, then surfaces success/failure.
     * Keeping the IO out of the ViewModel is what lets this code live in commonMain.
     */
    data class ExportReady(val json: String) : ProfileEffect
}

sealed interface ProfileMessage {
    data object SessionClosed : ProfileMessage

    /**
     * [com.emm.domain.auth.SignOutUseCase] returned [com.emm.domain.auth.SignOutResult.LocalOnly]:
     * the local session was cleared, but the server-side revoke could not be reached. Distinct from
     * [SessionClosed] so the snackbar can say the server was not reached, rather than implying a
     * clean revoke that did not happen.
     */
    data object SessionClosedLocallyOnly : ProfileMessage
    data object AccountDeleted : ProfileMessage
    data object ExportDone : ProfileMessage
    data object ExportFailed : ProfileMessage
    data class ImportDone(val transactions: Int, val recurring: Int) : ProfileMessage
    data object ImportFailed : ProfileMessage

    /** A manually requested snapshot backup was uploaded, verified and recorded. */
    data object BackupDone : ProfileMessage

    /**
     * A manually requested snapshot backup ended without one.
     *
     * **One message for every failure, and it is not derived from the exception.** See
     * [ProfileViewModel.onBackupEvent]: the account-switch refusal arrives as
     * `DomainException.Unauthorized`, which the app's two existing readings of that type would turn
     * into "sesión expirada" and a forced sign-out. Finer-grained backup failure copy belongs to
     * ADR 009 Phase 3, together with the health surface that gives it somewhere to live.
     */
    data object BackupFailed : ProfileMessage

    /**
     * The tap was refused because there is no session to back up to. Refused here rather than
     * queued, because `BackupOrchestrator.requestBackup` discards a session-less manual request
     * without reporting anything — this ViewModel is the only place that sees both the tap and the
     * session.
     */
    data object BackupNeedsAccount : ProfileMessage

    /**
     * A re-entry guard fired: another op was already in flight. One generic message for every op on
     * purpose — see [ProfileViewModel.launchOp]'s own doc for why it does not grow a per-op branch,
     * and `ProfileViewModel.backUpNow`, which repeats the check without going through it.
     */
    data object OperationInProgress : ProfileMessage
}
