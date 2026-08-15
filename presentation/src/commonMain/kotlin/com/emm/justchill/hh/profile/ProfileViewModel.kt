package com.emm.justchill.hh.profile

import androidx.lifecycle.viewModelScope
import com.emm.domain.account.AccountRepository
import com.emm.domain.auth.DeleteUserAccountUseCase
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.auth.SignOutResult
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.category.CategoryRepository
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.shared.backup.ImportDataUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.justchill.core.backup.BackupController
import com.emm.justchill.core.backup.BackupEvent
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.core.sync.SYNC_TEMPORARILY_DISABLED
import com.emm.justchill.core.sync.SyncController
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.time.Clock

@Suppress("LongParameterList")
class ProfileViewModel(
    private val backupRepository: BackupRepository,
    private val importData: ImportDataUseCase,
    private val signOut: SignOutUseCase,
    private val deleteUserAccount: DeleteUserAccountUseCase,
    private val syncController: SyncController,
    private val backupController: BackupController,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    observeSession: ObserveSessionUseCase,
    // Stamped into the backup payload. Injected (no BuildConfig in commonMain) by the
    // platform Koin module via the "appVersion" qualifier.
    private val appVersion: String,
    // Stamps `exportedAt` on the backup payload. No default, because ProfileModule builds this
    // class by hand and a default is one the wiring can silently keep using instead of the bound
    // Clock — which is exactly what happened here. AppGraphKoinTest now fails if it happens again.
    // Tests were never the reason for removing it: they can pass a clock either way.
    private val clock: Clock,
) : MviViewModel<ProfileUiState, ProfileIntent, ProfileEffect>() {

    override val initialState = ProfileUiState()

    init {
        combine(
            categoryRepository.all(),
            accountRepository.all(),
        ) { categories, accounts ->
            categories.size to accounts.size
        }
            .onEach { (catCount, accCount) ->
                updateState { copy(categoryCount = catCount, accountCount = accCount) }
            }
            .launchIn(viewModelScope)

        observeSession()
            .onEach { status ->
                val sessionUiState = when (status) {
                    is SessionStatus.Authenticated -> SessionUiState.SignedIn(status.user.email)
                    SessionStatus.NotAuthenticated -> SessionUiState.SignedOut
                    SessionStatus.Initializing -> SessionUiState.Initializing
                }
                updateState { copy(session = sessionUiState) }
            }
            .launchIn(viewModelScope)

        syncController.status
            .onEach { syncStatus ->
                val row = when {
                    syncStatus.isSyncing -> SyncRowUi.Syncing
                    syncStatus.lastSyncFailed -> SyncRowUi.Failed
                    else -> SyncRowUi.Idle(syncStatus.lastSyncedAtMillis)
                }
                updateState {
                    copy(
                        isSyncing = syncStatus.isSyncing,
                        syncRow = row,
                    )
                }
            }
            .launchIn(viewModelScope)

        backupController.isBackingUp
            .onEach(::onBackupProgress)
            .launchIn(viewModelScope)

        backupController.events
            .onEach(::onBackupEvent)
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.ExportRequested -> exportRequested()
            is ProfileIntent.ImportJson -> importFromJson(intent.json)
            ProfileIntent.SignOut -> performSignOut()
            ProfileIntent.SyncNow -> syncNow()
            ProfileIntent.DeleteAccount -> deleteAccount()
            ProfileIntent.BackUpNow -> backUpNow()
        }
    }

    /**
     * Mirrors the orchestrator's cycle flag onto the shared [ProfileUiState.op] slot — claiming it
     * only while it is free, releasing only what it claimed.
     *
     * `BackupController.isBackingUp` reports **every** cycle, and the automatic ones fire from the
     * app's own lifecycle: one can start while an export or an import is already running here. A
     * blind `copy(op = BackingUp)` would overwrite that op, and then [launchOp]'s `finally` would
     * reset the slot to `None` while the backup was still in flight — two operations sharing one
     * field with neither owning it, which reads on screen as an export that finished twice.
     *
     * So an automatic backup landing mid-export is simply not shown. That is the honest of the two
     * failures: the row keeps describing the operation the user started, and the backup is not
     * something they are waiting on.
     */
    private fun onBackupProgress(backingUp: Boolean) = updateState {
        when {
            backingUp && op == ProfileOp.None -> copy(op = ProfileOp.BackingUp)
            !backingUp && op == ProfileOp.BackingUp -> copy(op = ProfileOp.None)
            else -> this
        }
    }

    /**
     * Answers a manual backup — and **no backup failure signs the user out. None.**
     *
     * [BackupEvent.Failed] carries a `DomainException` and this deliberately does not read it. The
     * account-switch refusal `DefaultBackupUploader` throws is a `DomainException.Unauthorized`, and
     * that type cannot be told apart from a genuinely expired session, so both existing readings of
     * it are wrong here: `toUserMessage()` renders it as *"Credenciales incorrectas o sesión
     * expirada"*, and `SyncOrchestrator`'s posture for it is sign out + `SyncEvent.SessionExpired`.
     * Either inherited blindly force-signs-out a user whose only crime was switching accounts —
     * while signed in, on a device holding real data.
     *
     * One message for every failure is therefore the deliberate floor, not the ceiling: ADR 009
     * Phase 3 owns backup health, and finer-grained copy needs the state surface it builds to be
     * worth anything. Note that this is also why a failure comes back as
     * [ProfileEffect.Notify] and never [ProfileEffect.ShowError] — the latter is the effect that
     * routes through `toUserMessage()`.
     */
    private fun onBackupEvent(event: BackupEvent) {
        val message: ProfileMessage = when (event) {
            BackupEvent.Succeeded -> ProfileMessage.BackupDone
            is BackupEvent.Failed -> ProfileMessage.BackupFailed
        }
        sendEffect(ProfileEffect.Notify(message))
    }

    /**
     * The "Respaldar ahora" tap.
     *
     * **Not a [launchOp] call, on purpose.** That helper measures a suspend block's completion, and
     * `requestBackup` returns the instant the request is queued — wrapping it would report success
     * for a backup that has not started. What this reuses is [launchOp]'s *re-entry guard*, in the
     * same shape and for the same reason: it is what stops a tap from racing an export or an import.
     * Concurrency between backup cycles themselves is not this class's business at all — the
     * orchestrator's conflated channel and single consumer own that.
     *
     * The signed-out refusal is answered here because nobody else can. `requestBackup` discards a
     * session-less manual request rather than deferring it, and reports nothing (argued in its own
     * KDoc); this is the only place that sees both the tap and the session.
     */
    private fun backUpNow() {
        val refusal: ProfileMessage? = when {
            currentState.op != ProfileOp.None -> ProfileMessage.OperationInProgress
            currentState.session !is SessionUiState.SignedIn -> ProfileMessage.BackupNeedsAccount
            else -> null
        }
        if (refusal == null) {
            backupController.requestBackup(manual = true)
        } else {
            sendEffect(ProfileEffect.Notify(refusal))
        }
    }

    /**
     * Runs one profile operation with the shared lifecycle: guard against any
     * concurrent op, raise [ProfileUiState.op], run [block], always reset.
     * try/finally guarantees the reset on success, domain error (rethrown to
     * launchSafe's handler), and coroutine cancellation.
     *
     * The guard reports instead of swallowing: a confirmed intent arriving while another op is in
     * flight used to return silently with no effect and no state change — indistinguishable on
     * screen from the delete-account RPC never firing at all (`docs/archive/sync/AUDIT.md` §8, candidate 1).
     * One generic [ProfileMessage.OperationInProgress] covers every op here on purpose — this
     * guard is shared, and must not grow a per-op branch. [backUpNow] repeats the check instead of
     * calling this helper (it has no suspend block to measure) and reuses the message for the same
     * reason.
     */
    private fun launchOp(op: ProfileOp, onError: (DomainException) -> ProfileEffect, block: suspend () -> Unit) {
        // Near-unreachable on Android by hand: all four entry points also gate on `state.op` in
        // ProfileScreen.kt, so only a sub-frame double-dispatch race reaches this. Keep it anyway —
        // those Compose guards are per-platform, this is the shared `:presentation` backstop, and
        // iOS slice S9 consumes this ViewModel with none of them.
        if (currentState.op != ProfileOp.None) {
            sendEffect(ProfileEffect.Notify(ProfileMessage.OperationInProgress))
            return
        }
        updateState { copy(op = op) }
        launchSafe(onError = onError) {
            try {
                block()
            } finally {
                updateState { copy(op = ProfileOp.None) }
            }
        }
    }

    private fun performSignOut() = launchOp(
        op = ProfileOp.SigningOut,
        onError = { e -> ProfileEffect.ShowError(e) },
    ) {
        // Local data is intentionally NOT wiped on sign-out (see SignOutUseCase doc).
        val message = when (signOut.invoke()) {
            SignOutResult.Revoked -> ProfileMessage.SessionClosed
            SignOutResult.LocalOnly -> ProfileMessage.SessionClosedLocallyOnly
        }
        sendEffect(ProfileEffect.Notify(message))
    }

    private fun deleteAccount() = launchOp(
        op = ProfileOp.DeletingAccount,
        onError = { e -> ProfileEffect.ShowError(e) },
    ) {
        deleteUserAccount.invoke()
        sendEffect(ProfileEffect.Notify(ProfileMessage.AccountDeleted))
    }

    private fun exportRequested() = launchOp(
        op = ProfileOp.Exporting,
        // Failures here are domain errors from generating the backup (DB read / serialize).
        // The disk-space / write failure is the platform layer's concern and is surfaced there.
        onError = { e -> ProfileEffect.ShowError(e) },
    ) {
        val json = backupRepository.exportToJson(
            exportedAt = clock.now().toEpochMilliseconds(),
            appVersion = appVersion,
        )
        sendEffect(ProfileEffect.ExportReady(json))
    }

    private fun syncNow() {
        // Kill switch: the manual path stops at its origin instead of enqueueing a request that no
        // consumer exists to drain. See SyncKillSwitch.kt.
        if (SYNC_TEMPORARILY_DISABLED) return
        syncController.requestSync(manual = true)
    }

    private fun importFromJson(json: String) = launchOp(
        op = ProfileOp.Importing,
        onError = { e ->
            // ValidationError carries a user-actionable message (corrupt file, unsupported version);
            // anything else collapses to a generic "file might be damaged" hint.
            when (e) {
                is DomainException.ValidationError -> ProfileEffect.ShowError(e)
                else -> ProfileEffect.Notify(ProfileMessage.ImportFailed)
            }
        },
    ) {
        val stats = importData(json)
        sendEffect(ProfileEffect.Notify(ProfileMessage.ImportDone(stats.transactions, stats.recurring)))
    }
}
