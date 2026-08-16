package com.emm.justchill.hh.profile

import androidx.lifecycle.viewModelScope
import com.emm.domain.account.AccountRepository
import com.emm.domain.auth.DeleteUserAccountUseCase
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.auth.SignOutResult
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.category.CategoryRepository
import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.shared.backup.BackupVerification
import com.emm.domain.shared.backup.BackupVerifier
import com.emm.domain.shared.backup.GetBackupStalenessUseCase
import com.emm.domain.shared.backup.ImportDataUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.logging.DiagnosticsLogger
import com.emm.justchill.core.backup.BackupController
import com.emm.justchill.core.backup.BackupEvent
import com.emm.justchill.core.backup.BackupHealth
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.core.sync.SYNC_TEMPORARILY_DISABLED
import com.emm.justchill.core.sync.SyncController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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
    private val backupVerifier: BackupVerifier,
    private val getBackupStaleness: GetBackupStalenessUseCase,
    private val logger: DiagnosticsLogger,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    observeSession: ObserveSessionUseCase,
    private val appVersion: String,
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
            .onEach { event -> sendEffect(ProfileEffect.Notify(event.toProfileMessage())) }
            .launchIn(viewModelScope)

        combine(
            state.map { it.session }.distinctUntilChanged(),
            backupController.health,
            backupController.isBackingUp,
            ::Triple,
        )
            .onEach { (sessionUiState, health, backingUp) ->
                val row: BackupRowUi = resolveBackupRow(sessionUiState, health, backingUp, getBackupStaleness, logger)
                updateState { copy(backupRow = row) }
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
            ProfileIntent.BackUpNow -> backUpNow()
            ProfileIntent.VerifyBackup -> verifyBackup()
            ProfileIntent.AcknowledgeBackupDestination -> acknowledgeBackupDestination()
        }
    }

    private fun onBackupProgress(backingUp: Boolean) = updateState {
        when {
            backingUp && op == ProfileOp.None -> copy(op = ProfileOp.BackingUp)
            !backingUp && op == ProfileOp.BackingUp -> copy(op = ProfileOp.None)
            else -> this
        }
    }

    private fun backUpNow() {
        val refusal: ProfileMessage? = when {
            currentState.op != ProfileOp.None -> ProfileMessage.OperationInProgress
            currentState.session !is SessionUiState.SignedIn -> ProfileMessage.BackupNeedsAccount
            currentState.backupRow == BackupRowUi.DisclosurePending -> ProfileMessage.BackupNeedsDisclosure
            else -> null
        }
        if (refusal == null) {
            backupController.requestBackup(manual = true)
        } else {
            sendEffect(ProfileEffect.Notify(refusal))
        }
    }

    // Not a backup cycle: it never touches the streak, the watermark or backup health, so it takes
    // the generic op slot instead of backUpNow's ladder, and a failure is a Notify like every other
    // backup failure — never a ShowError, which would read as expired credentials.
    private fun verifyBackup() = launchOp(
        op = ProfileOp.VerifyingBackup,
        onError = { ProfileEffect.Notify(ProfileMessage.BackupVerifyFailed) },
    ) {
        sendEffect(ProfileEffect.Notify(backupVerifier.verifyLatest().toProfileMessage()))
    }

    // The disclosure is written regardless of op, so the user's acknowledgement is never lost; only
    // the follow-on cycle is gated, the same guard backUpNow applies to a direct tap.
    private fun acknowledgeBackupDestination() {
        backupController.acknowledgeDestination(requestCycle = currentState.op == ProfileOp.None)
    }

    private fun launchOp(op: ProfileOp, onError: (DomainException) -> ProfileEffect, block: suspend () -> Unit) {
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
        onError = { e -> ProfileEffect.ShowError(e) },
    ) {
        val json = backupRepository.exportToJson(
            exportedAt = clock.now().toEpochMilliseconds(),
            appVersion = appVersion,
        )
        sendEffect(ProfileEffect.ExportReady(json))
    }

    private fun syncNow() {
        if (SYNC_TEMPORARILY_DISABLED) return
        syncController.requestSync(manual = true)
    }

    private fun importFromJson(json: String) = launchOp(
        op = ProfileOp.Importing,
        onError = { e ->
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

private fun BackupEvent.toProfileMessage(): ProfileMessage = when (this) {
    BackupEvent.Succeeded -> ProfileMessage.BackupDone
    is BackupEvent.Failed -> ProfileMessage.BackupFailed
}

private fun BackupVerification.toProfileMessage(): ProfileMessage = when (this) {
    BackupVerification.NoSnapshots -> ProfileMessage.BackupNotVerified(pairsInspected = 0)
    is BackupVerification.Verified -> ProfileMessage.BackupVerified(this)
    is BackupVerification.NothingVerified -> ProfileMessage.BackupNotVerified(pairsInspected)
}

/**
 * ```
 * resolveBackupRow          not signed in ............... NeedsAccount
 *                           destination undisclosed ..... DisclosurePending
 *                           a cycle is running .......... BackingUp
 *                           no watermark ................ failing ? Failed(reason, None) : Never
 *                           a watermark ................. ↓
 * snapshotRow               the staleness read threw .... failing ? Failed(reason, AgeUnknown)
 *                                                                 : Unreadable
 *                           otherwise ................... failing ? Failed(reason, DaysAgo(…))
 *                                                                 : isStale ? Stale : UpToDate
 * ```
 */
private suspend fun resolveBackupRow(
    sessionUiState: SessionUiState,
    health: BackupHealth,
    backingUp: Boolean,
    getBackupStaleness: GetBackupStalenessUseCase,
    logger: DiagnosticsLogger,
): BackupRowUi {
    val failing: Boolean = health.consecutiveFailures > 0
    val lastSuccessfulBackupAt: Long? = health.lastSuccessfulBackupAt
    return when {
        sessionUiState !is SessionUiState.SignedIn -> BackupRowUi.NeedsAccount

        // Above BackingUp for the same reason NeedsAccount is: the orchestrator raises isBackingUp
        // for the whole cycle, including the one that is about to refuse, so ranking it lower would
        // flash "Respaldando…" over a device that is uploading nothing.
        !health.canUploadToDestination -> BackupRowUi.DisclosurePending

        backingUp -> BackupRowUi.BackingUp

        lastSuccessfulBackupAt == null ->
            if (failing) BackupRowUi.Failed(health.lastFailureReason, LastSnapshot.None) else BackupRowUi.Never

        else -> snapshotRow(lastSuccessfulBackupAt, failing, health.lastFailureReason, getBackupStaleness, logger)
    }
}

// Intentional broad catch: a frozen Perfil screen is the alternative — see snapshotRow's callers.
@Suppress("TooGenericExceptionCaught")
private suspend fun snapshotRow(
    lastSuccessfulBackupAt: Long,
    failing: Boolean,
    reason: BackupFailureReason?,
    getBackupStaleness: GetBackupStalenessUseCase,
    logger: DiagnosticsLogger,
): BackupRowUi {
    val staleness = try {
        getBackupStaleness(lastSuccessfulBackupAt)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.warn(
            "could not read backup staleness for the Perfil row; the last-backup watermark is " +
                "$lastSuccessfulBackupAt and the row falls back to an undated snapshot rather than " +
                "claiming a health it could not determine",
            e,
        )
        return if (failing) BackupRowUi.Failed(reason, LastSnapshot.AgeUnknown) else BackupRowUi.Unreadable
    }
    val snapshot = LastSnapshot.DaysAgo(days = staleness.daysSinceLastBackup, isStale = staleness.isStale)
    return when {
        failing -> BackupRowUi.Failed(reason, snapshot)
        staleness.isStale -> BackupRowUi.Stale(staleness.daysSinceLastBackup)
        else -> BackupRowUi.UpToDate(staleness.daysSinceLastBackup)
    }
}
