package com.emm.justchill.feature.profile

import com.emm.justchill.core.domain.auth.DeleteUserAccountUseCase
import com.emm.justchill.core.domain.auth.ObserveSessionUseCase
import com.emm.justchill.core.domain.auth.SessionStatus
import com.emm.justchill.core.domain.auth.SignOutResult
import com.emm.justchill.core.domain.auth.SignOutUseCase
import com.emm.justchill.core.domain.category.CategoryRepository
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.recurring.GetRecurringMonthlySummaryUseCase
import com.emm.justchill.core.domain.shared.backup.BackupController
import com.emm.justchill.core.domain.shared.backup.BackupEvent
import com.emm.justchill.core.domain.shared.backup.BackupFailureReason
import com.emm.justchill.core.domain.shared.backup.BackupHealth
import com.emm.justchill.core.domain.shared.backup.BackupRepository
import com.emm.justchill.core.domain.shared.backup.BackupVerification
import com.emm.justchill.core.domain.shared.backup.BackupVerifier
import com.emm.justchill.core.domain.shared.backup.ExportHistory
import com.emm.justchill.core.domain.shared.backup.GetBackupStalenessUseCase
import com.emm.justchill.core.domain.shared.backup.ImportDataUseCase
import com.emm.justchill.core.domain.shared.backup.toBackupFailureReason
import com.emm.justchill.core.domain.shared.error.DomainException
import com.emm.justchill.core.domain.shared.logging.DiagnosticsLogger
import com.emm.justchill.core.domain.time.TodayFlow
import com.emm.justchill.core.ui.mvi.MviViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

@Suppress("LongParameterList")
class ProfileViewModel(
    private val backupRepository: BackupRepository,
    private val importData: ImportDataUseCase,
    private val signOut: SignOutUseCase,
    private val deleteUserAccount: DeleteUserAccountUseCase,
    private val backupController: BackupController,
    private val backupVerifier: BackupVerifier,
    private val getBackupStaleness: GetBackupStalenessUseCase,
    private val logger: DiagnosticsLogger,
    private val localExportHistory: ExportHistory,
    private val todayFlow: TodayFlow,
    categoryRepository: CategoryRepository,
    getRecurringMonthlySummary: GetRecurringMonthlySummaryUseCase,
    observeSession: ObserveSessionUseCase,
    private val appVersion: String,
    private val clock: Clock,
) : MviViewModel<ProfileUiState, ProfileIntent, ProfileEffect>(
    ProfileUiState(lastExport = localExportHistory.toLastExportUi(todayFlow.today())),
) {
    private val onDomainError: (DomainException) -> ProfileEffect = ProfileEffect::ShowError

    init {
        todayFlow()
            .onEach { today -> updateState { copy(lastExport = localExportHistory.toLastExportUi(today)) } }
            .launchSafeIn(onError = onDomainError)

        categoryRepository.all()
            .onEach { categories ->
                updateState {
                    copy(
                        categoryCount = categories.size,
                        incomeCategoryCount = categories.count { it.categoryType == CategoryType.Income },
                    )
                }
            }
            .launchSafeIn(onError = onDomainError)

        getRecurringMonthlySummary()
            .onEach { summary ->
                updateState {
                    copy(recurringCount = summary.activeCount, recurringMonthlyOutflow = summary.monthlyOutflow)
                }
            }
            .launchSafeIn(onError = onDomainError)

        observeSession()
            .onEach { status ->
                val sessionUiState = when (status) {
                    is SessionStatus.Authenticated -> SessionUiState.SignedIn(status.user.email)
                    SessionStatus.NotAuthenticated -> SessionUiState.SignedOut
                    SessionStatus.Initializing -> SessionUiState.Initializing
                }
                updateState { copy(session = sessionUiState) }
            }
            .launchSafeIn(onError = onDomainError)

        backupController.isBackingUp
            .onEach(::onBackupProgress)
            .launchSafeIn(onError = onDomainError)

        backupController.events
            .onEach { event -> sendEffect(ProfileEffect.Notify(event.toProfileMessage())) }
            .launchSafeIn(onError = onDomainError)

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
            .launchSafeIn(onError = onDomainError)
    }

    override fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.ExportRequested -> exportRequested()
            is ProfileIntent.ExportFinished -> exportFinished(intent.saved)
            is ProfileIntent.ImportJson -> importFromJson(intent.json)
            ProfileIntent.SignOut -> performSignOut()
            ProfileIntent.DeleteAccountClicked -> updateState { copy(dialog = ProfileDialog.DeleteAccount) }
            ProfileIntent.DeleteAccountConfirmed -> deleteAccount()
            ProfileIntent.ImportClicked -> updateState { copy(dialog = ProfileDialog.Import) }
            ProfileIntent.DialogDismissed -> updateState { copy(dialog = ProfileDialog.None) }
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

    // Not a backup cycle: a failure is a Notify like every other backup failure, never a ShowError,
    // which would read as expired credentials.
    private fun verifyBackup() {
        val refusal: ProfileMessage? = when {
            currentState.op != ProfileOp.None -> ProfileMessage.OperationInProgress
            currentState.session !is SessionUiState.SignedIn -> ProfileMessage.BackupNeedsAccount
            else -> null
        }
        if (refusal != null) {
            sendEffect(ProfileEffect.Notify(refusal))
            return
        }
        launchOp(
            op = ProfileOp.VerifyingBackup,
            onError = { ProfileEffect.Notify(ProfileMessage.BackupVerifyFailed) },
        ) {
            sendEffect(ProfileEffect.Notify(backupVerifier.verifyLatest().toProfileMessage()))
        }
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
        onError = onDomainError,
    ) {
        val message = when (signOut.invoke()) {
            SignOutResult.Revoked -> ProfileMessage.SessionClosed
            SignOutResult.LocalOnly -> ProfileMessage.SessionClosedLocallyOnly
        }
        sendEffect(ProfileEffect.Notify(message))
    }

    private fun deleteAccount() {
        updateState { copy(dialog = ProfileDialog.None) }
        launchOp(
            op = ProfileOp.DeletingAccount,
            onError = onDomainError,
        ) {
            deleteUserAccount.invoke()
            sendEffect(ProfileEffect.Notify(ProfileMessage.AccountDeleted))
        }
    }

    private fun exportRequested() = launchOp(
        op = ProfileOp.Exporting,
        onError = onDomainError,
    ) {
        val json = backupRepository.exportToJson(
            exportedAt = clock.now().toEpochMilliseconds(),
            appVersion = appVersion,
        )
        sendEffect(ProfileEffect.ExportReady(json))
    }

    private fun exportFinished(saved: Boolean) {
        if (saved) {
            localExportHistory.recordExport()
            updateState { copy(lastExport = localExportHistory.toLastExportUi(todayFlow.today())) }
        }
        sendEffect(ProfileEffect.Notify(if (saved) ProfileMessage.ExportDone else ProfileMessage.ExportFailed))
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
        sendEffect(
            ProfileEffect.Notify(
                ProfileMessage.ImportDone(
                    transactions = stats.transactions,
                    recurring = stats.recurring,
                    loans = stats.loans,
                    loanPayments = stats.loanPayments,
                ),
            ),
        )
    }
}

private fun ExportHistory.toLastExportUi(today: LocalDate): LastExportUi =
    daysSinceLastExport(today)?.let(LastExportUi::DaysAgo) ?: LastExportUi.Never

private fun BackupEvent.toProfileMessage(): ProfileMessage = when (this) {
    BackupEvent.Succeeded -> ProfileMessage.BackupDone
    is BackupEvent.Failed -> ProfileMessage.BackupFailed(cause.toBackupFailureReason())
}

private fun BackupVerification.toProfileMessage(): ProfileMessage = when (this) {
    BackupVerification.NoSnapshots -> ProfileMessage.BackupNotVerified(pairsInspected = 0)
    is BackupVerification.Verified -> ProfileMessage.BackupVerified(this)
    is BackupVerification.NothingVerified -> ProfileMessage.BackupNotVerified(pairsInspected)
}

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
