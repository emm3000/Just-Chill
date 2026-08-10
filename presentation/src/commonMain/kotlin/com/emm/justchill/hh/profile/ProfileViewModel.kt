package com.emm.justchill.hh.profile

import androidx.lifecycle.viewModelScope
import com.emm.domain.account.AccountRepository
import com.emm.domain.auth.DeleteUserAccountUseCase
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.category.CategoryRepository
import com.emm.domain.shared.backup.ExportDataUseCase
import com.emm.domain.shared.backup.ImportDataUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.core.sync.SyncController
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.time.Clock

@Suppress("LongParameterList")
class ProfileViewModel(
    private val exportData: ExportDataUseCase,
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
    private val clock: Clock = Clock.System,
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
     */
    private fun launchOp(op: ProfileOp, onError: (DomainException) -> ProfileEffect, block: suspend () -> Unit) {
        if (currentState.op != ProfileOp.None) return
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
        signOut.invoke()
        // Local data is intentionally NOT wiped on sign-out (see SignOutUseCase doc).
        sendEffect(ProfileEffect.Notify(ProfileMessage.SessionClosed))
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
        val json = exportData(
            exportedAt = clock.now().toEpochMilliseconds(),
            appVersion = appVersion,
        )
        sendEffect(ProfileEffect.ExportReady(json))
    }

    private fun syncNow() {
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
