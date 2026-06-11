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
import com.emm.justchill.BuildConfig
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.core.sync.SyncOrchestrator
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.OutputStream

@Suppress("LongParameterList")
class ProfileViewModel(
    private val exportData: ExportDataUseCase,
    private val importData: ImportDataUseCase,
    private val signOut: SignOutUseCase,
    private val deleteUserAccount: DeleteUserAccountUseCase,
    private val syncOrchestrator: SyncOrchestrator,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    observeSession: ObserveSessionUseCase,
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

        syncOrchestrator.status
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
            is ProfileIntent.ExportToStream -> exportToStream(intent.output)
            is ProfileIntent.ImportJson -> importFromJson(intent.json)
            ProfileIntent.SignOut -> signOut()
            ProfileIntent.SyncNow -> syncNow()
            ProfileIntent.DeleteAccount -> deleteAccount()
        }
    }

    private fun signOut() = launchSafe(
        onError = { e -> ProfileEffect.ShowMessage(e.toUserMessage()) },
    ) {
        signOut.invoke()
        // Local data is intentionally NOT wiped on sign-out (see SignOutUseCase doc).
        sendEffect(ProfileEffect.ShowMessage("Sesión cerrada. Tus datos siguen en este teléfono."))
    }

    private fun deleteAccount() {
        // Guard against re-fires while the remote delete is in flight (dialog can be reopened).
        if (state.value.isDeletingAccount) return
        updateState { copy(isDeletingAccount = true) }
        launchSafe(
            onError = { e ->
                updateState { copy(isDeletingAccount = false) }
                ProfileEffect.ShowMessage(e.toUserMessage())
            },
        ) {
            deleteUserAccount.invoke()
            updateState { copy(isDeletingAccount = false) }
            sendEffect(ProfileEffect.ShowMessage("Cuenta eliminada. Tus datos siguen en este teléfono."))
        }
    }

    private fun exportToStream(output: OutputStream) = launchSafe(
        onError = { e ->
            // Domain errors carry their own user-facing message (e.g. DatabaseError);
            // Unknown collapses to a disk-space hint, the most plausible cause for export IO failures.
            val msg = when (e) {
                is DomainException.Unknown -> "No pude exportar — capaz no hay espacio en tu celu?"
                else -> e.toUserMessage()
            }
            ProfileEffect.ShowMessage(msg)
        },
    ) {
        updateState { copy(isExporting = true) }
        try {
            val json = exportData(
                exportedAt = System.currentTimeMillis(),
                appVersion = BuildConfig.VERSION_NAME,
            )
            // Writer is closed here — not at the launcher callsite — because the launcher
            // hands us a raw stream and we schedule async work; closing it early would corrupt the write.
            // Closing the BufferedWriter flushes its buffer to the stream before closing.
            output.bufferedWriter().use { it.write(json) }
            sendEffect(ProfileEffect.ShowMessage("Listo, tu data está guardada."))
        } finally {
            updateState { copy(isExporting = false) }
        }
    }

    private fun syncNow() {
        syncOrchestrator.requestSync(manual = true)
    }

    private fun importFromJson(json: String) = launchSafe(
        onError = { e ->
            // ValidationError carries a user-actionable message (corrupt file, unsupported version);
            // anything else collapses to a generic "file might be damaged" hint.
            val msg = when (e) {
                is DomainException.ValidationError -> e.toUserMessage()
                else -> "No pude importar el archivo — capaz está dañado."
            }
            ProfileEffect.ShowMessage(msg)
        },
    ) {
        updateState { copy(isImporting = true) }
        try {
            val stats = importData(json)
            sendEffect(ProfileEffect.ShowMessage("Listo — ${stats.transactions} movimientos importados."))
        } finally {
            updateState { copy(isImporting = false) }
        }
    }
}
