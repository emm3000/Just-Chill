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
    private val getBackupStaleness: GetBackupStalenessUseCase,
    // Keeps the backup row's swallowed failure observable. The orchestrator logs its own cycles;
    // without this, the ONE read this class makes on that path would fail silently — in the unit
    // whose whole purpose is health visibility.
    private val logger: DiagnosticsLogger,
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

        // Separate from the isBackingUp collector above even though both read that flag: that one
        // owns the shared `op` slot and must not overwrite another operation, this one owns a row
        // nothing else writes. Combining them would put the op guard's exception on the health row.
        //
        // The session input is `state` itself rather than a second MutableStateFlow mirroring it.
        // A mirror is two writes that have to agree forever with nothing enforcing it, and there is
        // no re-entrancy to avoid: this collector's own `updateState` re-emits `state`, but
        // `distinctUntilChanged` over the mapped session drops that echo, so the combine cannot
        // feed itself. It also cannot see a session the account row has not already rendered,
        // which is stronger than what the mirror gave.
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
     * same shape and for the same reason — but it only guards **this** direction: it stops a backup
     * tap from racing an export or an import already in flight. It does not stop the reverse.
     * `requestBackup` returns before [ProfileUiState.op] flips to [ProfileOp.BackingUp], so for a
     * dispatch hop right after this tap `op` is still `None` and an export or import can still start
     * on top of it. Benign in practice — a two-row double-tap inside a few milliseconds, and SQLite
     * serialises the transactions — so this is not something to redesign the guard around.
     * Concurrency between backup cycles themselves is not this class's business at all — the
     * orchestrator's conflated channel and single consumer own that.
     *
     * The signed-out refusal here is a fast local answer, not the only one. `BackupOrchestrator
     * .requestBackup` still queues a session-less manual request rather than refusing it outright;
     * its own consumer answers one with [BackupEvent.Failed] once it drains it, which is what covers
     * the session ending in the gap between this check and that draining. What this method buys is
     * the common case — a tap refused before it ever reaches the channel — and both answers render
     * identically on screen, so nothing distinguishes them from the user's side.
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

/**
 * The whole of the "Último respaldo" row's discrimination, in the order [BackupRowUi] documents.
 *
 * Private top-level rather than a method, in the shape `BackupOrchestrator.kt` already uses for its
 * own decision helpers: it reads nothing off the ViewModel but the collaborators it is handed, and
 * `ProfileViewModel` sits on detekt's per-class function limit.
 *
 * ### Order, and what each position is protecting
 *
 * **[BackupRowUi.NeedsAccount] is first, ahead of [BackupRowUi.BackingUp].** A cycle in flight when
 * the session ends would otherwise render "Respaldando…" to a signed-out user, describing work
 * being done for an account they just left. Without a session no other branch means anything
 * either: the health surface is per-account and publishes `None` the moment the gate closes.
 *
 * **A failure never replaces the last snapshot; it annotates it.** `takeSnapshot` skips the
 * due-check for a manual request, so "automatic backup succeeded at 09:00, user taps Respaldar
 * ahora at 10:00 on bad wifi" is ordinary — and the daily cap then blocks any automatic cycle from
 * clearing the streak until midnight. Ranking a failure above a fresh snapshot would hide the one
 * fact a row titled "Último respaldo" exists to show, for the rest of the day, on a device whose
 * data is perfectly safe. So the age is resolved first and handed to [BackupRowUi.Failed], which
 * carries both facts; the copy decides which one leads, and `lastBackupDaysAgo == null` — nothing
 * backed up at all AND failing — is the genuinely alarming case that keeps the loud wording.
 *
 * **[BackupRowUi.Failed] is chosen on `consecutiveFailures > 0`, never on
 * `lastFailureReason != null`** — `BackupHealth`'s KDoc spells out why: the reason degrades to null
 * for a persisted name this build cannot resolve, so `(5, null)` is a real value and keying on the
 * reason would show a device that has failed five times running as healthy. The reason is carried
 * through only to pick the wording.
 *
 * The staleness read is asked only where it can be answered — never without a session, and never
 * without the watermark that [GetBackupStalenessUseCase] refuses to take as null.
 *
 * The known limit, and it is the same one `docs/DATE_AUDIT.md` #7 records for
 * `ReportUiState.isCurrentMonth`: the day count is computed per emission, not continuously. A Perfil
 * left open across midnight keeps saying "Hoy" until the next emission from any of the three
 * sources. Refreshing on every state write is not the same as refreshing continuously.
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

        backingUp -> BackupRowUi.BackingUp

        lastSuccessfulBackupAt == null ->
            if (failing) BackupRowUi.Failed(health.lastFailureReason, lastBackupDaysAgo = null) else BackupRowUi.Never

        else -> snapshotRow(lastSuccessfulBackupAt, failing, health.lastFailureReason, getBackupStaleness, logger)
    }
}

/**
 * The branch that has a verified snapshot to describe, with everything that can throw contained.
 *
 * [GetBackupStalenessUseCase] runs inside a plain `onEach` collector rather than `launchSafe`, so an
 * escaping exception would cancel `viewModelScope` and take the session, sync and counter collectors
 * down with it, leaving Perfil frozen with no message. That is ADR 009's hard constraint 4 (no
 * invisible failure) wearing a dead screen.
 *
 * **The catch is broad on purpose, and an earlier version of it was not.** It used to catch
 * `DomainException` while claiming the database read was the only thing here that could throw. That
 * was false: the use case also converts an epoch-millis watermark to a `LocalDate`, and
 * `kotlinx.datetime` raises `DateTimeArithmeticException` — not a `DomainException` — for a value
 * outside the representable range. A corrupted preference is all that takes. `CancellationException`
 * is rethrown first, per `MviViewModel.launchSafe`'s reasoning: it is an `Exception`, and swallowing
 * it would break the collector's own cancellation.
 *
 * The fallback is [BackupRowUi.Unreadable] rather than a mapped [BackupRowUi.Failed]: the failure is
 * in *reading* the state, not in a backup cycle, and the two say different true things. Nothing here
 * knows whether that snapshot is fresh, so nothing here may imply it. The swallow is logged because
 * it is otherwise the one failure on this screen with no channel at all.
 */
// Intentional broad catch: see the KDoc — a frozen Perfil is the alternative.
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
                "$lastSuccessfulBackupAt and the row falls back to Unreadable rather than claiming " +
                "a health it could not determine",
            e,
        )
        return BackupRowUi.Unreadable
    }
    return when {
        failing -> BackupRowUi.Failed(reason, staleness.daysSinceLastBackup)
        staleness.isStale -> BackupRowUi.Stale(staleness.daysSinceLastBackup)
        else -> BackupRowUi.UpToDate(staleness.daysSinceLastBackup)
    }
}
