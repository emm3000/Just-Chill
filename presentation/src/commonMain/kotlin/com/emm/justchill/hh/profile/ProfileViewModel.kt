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
    }

    override fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.ExportRequested -> exportRequested()
            is ProfileIntent.ImportJson -> importFromJson(intent.json)
            ProfileIntent.SignOut -> performSignOut()
            ProfileIntent.SyncNow -> syncNow()
            ProfileIntent.DeleteAccount -> deleteAccount()
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
     * One generic [ProfileMessage.OperationInProgress] covers all four ops here on purpose — this
     * guard is shared, and must not grow a per-op branch.
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
        sendEffect(ProfileEffect.Notify(ProfileMessage.ImportDone(stats.transactions)))
    }
}
