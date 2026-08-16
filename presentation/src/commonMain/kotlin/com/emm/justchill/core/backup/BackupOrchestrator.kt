package com.emm.justchill.core.backup

import com.emm.data.backup.backupSnapshotName
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.domain.shared.backup.BackupFailureState
import com.emm.domain.shared.backup.BackupMetadataStore
import com.emm.domain.shared.backup.BackupPruner
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.shared.backup.BackupUploader
import com.emm.domain.shared.backup.hasLocalChangesSince
import com.emm.domain.shared.backup.toBackupFailureReason
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.logging.DiagnosticsLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// LongParameterList: twelve distinct collaborator ports/seams this class orchestrates; no holder
// would carry behaviour of its own, and the two lifecycle flows must stay independent parameters.
@Suppress("LongParameterList")
class BackupOrchestrator(
    private val backupRepository: BackupRepository,
    private val uploader: BackupUploader,
    private val pruner: BackupPruner,
    private val metadata: BackupMetadataStore,
    private val observeSession: ObserveSessionUseCase,
    private val appVersion: String,
    private val clock: Clock,
    private val timeZone: TimeZone,
    private val externalScope: CoroutineScope,
    private val backgroundEvents: Flow<Unit>,
    private val resumeEvents: Flow<Unit>,
    private val logger: DiagnosticsLogger,
) : BackupController {

    private val _isBackingUp = MutableStateFlow(false)
    override val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _events = MutableSharedFlow<BackupEvent>(replay = 0, extraBufferCapacity = 1)
    override val events: Flow<BackupEvent> = _events.asSharedFlow()

    private val _health = MutableStateFlow(BackupHealth.None)
    override val health: StateFlow<BackupHealth> = _health.asStateFlow()

    private val requestChannel = Channel<Unit>(Channel.CONFLATED)

    // @Volatile: written by the session collector, read by the request consumer on another thread.
    @Volatile
    private var currentUserId: String? = null

    // @Volatile: set from Main by requestBackup, cleared by runBackup — visibility only, not
    // atomicity. A manual tap landing in that read/write gap is swallowed rather than counted.
    @Volatile
    private var manualRequestPending = false

    // @Volatile: requestBackup reads this from Main while start() writes it once, synchronously,
    // before launching either coroutine below.
    @Volatile
    private var started = false

    override fun requestBackup(manual: Boolean) {
        if (!started) {
            logger.warn(
                "backup requested (manual=$manual) before the orchestrator started; the request is " +
                    "being dropped rather than queued onto a channel nothing drains",
            )
            return
        }
        if (manual) manualRequestPending = true
        requestChannel.trySend(Unit)
    }

    override fun acknowledgeDestination() {
        val userId: String = currentUserId ?: return
        metadata.setDestinationDisclosed(userId, clock.now().toEpochMilliseconds())
        publishHealth(userId, metadata.failureState(userId))
        requestBackup(manual = true)
    }

    fun start() {
        started = true

        externalScope.launch {
            for (ignored in requestChannel) {
                runBackup()
            }
        }

        launchResilientTrigger("lifecycle") {
            observeSession().flatMapLatest { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val userId = status.user.userId
                        currentUserId = userId
                        publishHealth(userId, metadata.failureState(userId))
                        merge(backgroundEvents, resumeEvents)
                    }

                    else -> {
                        currentUserId = null
                        _health.value = BackupHealth.None
                        flowOf()
                    }
                }
            }.collect { requestBackup() }
        }
    }

    // Intentional broad catch: this is the backstop that keeps a trigger failure off the app scope.
    @Suppress("TooGenericExceptionCaught")
    private fun launchResilientTrigger(name: String, block: suspend () -> Unit) {
        externalScope.launch {
            while (isActive) {
                try {
                    block()
                    logger.warn("backup trigger '$name' completed unexpectedly; not restarting")
                    return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.warn("backup trigger '$name' failed; restarting in $TRIGGER_RETRY_DELAY", e)
                    delay(TRIGGER_RETRY_DELAY)
                }
            }
        }
    }

    // Intentional broad catch: nothing may escape into externalScope; CancellationException is re-thrown.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun runBackup() {
        val manual = manualRequestPending
        manualRequestPending = false
        _isBackingUp.value = true
        val userId: String? = currentUserId
        try {
            if (userId == null) {
                if (manual) _events.tryEmit(BackupEvent.Failed(DomainException.Unauthorized(NO_SESSION)))
                return
            }
            report(manual, takeSnapshot(userId, manual))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val kind = if (manual) "manual" else "auto"
            val failure: DomainException = e.asDomainException()
            val reason: BackupFailureReason = failure.toBackupFailureReason()
            if (manual) _events.tryEmit(BackupEvent.Failed(failure))
            val streak: Int = if (userId == null) 0 else recordFailure(userId, reason)
            logger.warn(
                "backup cycle failed ($kind): reason=$reason, consecutive failures=$streak, " +
                    "${e::class.simpleName}. " +
                    "The last-successful watermark is untouched, so the next trigger retries.",
                e,
            )
        } finally {
            _isBackingUp.value = false
        }
    }

    private fun report(manual: Boolean, outcome: SnapshotOutcome) {
        if (!manual) return
        val event: BackupEvent? = when (outcome) {
            SnapshotOutcome.Recorded -> BackupEvent.Succeeded
            SnapshotOutcome.OwnerChanged -> BackupEvent.Failed(DomainException.Unauthorized(OWNER_CHANGED))
            SnapshotOutcome.NotDue,
            SnapshotOutcome.DestinationUndisclosed,
            -> null
        }
        if (event != null) _events.tryEmit(event)
    }

    private fun recordFailure(userId: String, reason: BackupFailureReason): Int {
        val state: BackupFailureState = metadata.recordFailure(userId, reason)
        publishHealth(userId, state)
        return state.consecutiveFailures
    }

    // Re-checked after the write: currentUserId can change on another thread between the read above
    // and this write; compareAndSet reverts only the value this call itself wrote.
    private fun publishHealth(userId: String, state: BackupFailureState) {
        if (currentUserId != userId) return
        val published = BackupHealth(
            lastSuccessfulBackupAt = metadata.lastSuccessfulBackupAt(userId),
            consecutiveFailures = state.consecutiveFailures,
            lastFailureReason = state.lastReason,
            isDestinationDisclosed = metadata.destinationDisclosedAt(userId) != null,
        )
        _health.value = published
        if (currentUserId != userId) _health.compareAndSet(published, BackupHealth.None)
    }

    private suspend fun takeSnapshot(userId: String, manual: Boolean): SnapshotOutcome {
        // Ahead of the due-check a manual request skips, so a tap cannot walk around the disclosure.
        if (metadata.destinationDisclosedAt(userId) == null) {
            logger.warn(
                "backup cycle refused: this device has not disclosed to $userId that its whole " +
                    "ledger, including rows written under a previous account, goes into this " +
                    "account's backup. Nothing is exported or uploaded until Perfil is acknowledged.",
            )
            return SnapshotOutcome.DestinationUndisclosed
        }

        val takenAt: Instant = clock.now()
        // manual skips isBackupDue: a user-requested backup runs regardless of dirty state or the
        // once-a-day cap.
        if (!manual && !isBackupDue(userId, takenAt)) return SnapshotOutcome.NotDue

        val payload: String = backupRepository.exportToJson(
            exportedAt = takenAt.toEpochMilliseconds(),
            appVersion = appVersion,
        )
        uploader.upload(userId, backupSnapshotName(takenAt), payload)
        // currentUserId read once and reused for both branches below: two reads of a @Volatile
        // field can disagree.
        val stillTheSameAccount: Boolean = currentUserId == userId
        if (stillTheSameAccount) {
            metadata.setLastSuccessfulBackupAt(userId, takenAt.toEpochMilliseconds())
            metadata.clearFailures(userId)
            publishHealth(userId, BackupFailureState.None)
            prune()
        } else {
            logger.warn(
                "backup upload finished for an account that is no longer signed in; the watermark " +
                    "is NOT recorded, the failure streak is left exactly as it was, and retention " +
                    "was skipped. Recording the watermark would mark $userId backed up for the day " +
                    "on the strength of a snapshot this device can no longer see, and suppress the " +
                    "real one.",
            )
        }
        return if (stillTheSameAccount) SnapshotOutcome.Recorded else SnapshotOutcome.OwnerChanged
    }

    private suspend fun isBackupDue(userId: String, now: Instant): Boolean {
        val lastSuccessAt: Long? = metadata.lastSuccessfulBackupAt(userId)
        return hasLocalChangesSince(backupRepository.latestLocalChangeAt(), lastSuccessAt) &&
            !alreadyBackedUpOn(lastSuccessAt, now, timeZone)
    }

    // Intentional broad catch: a prune must never undo or fail a verified upload.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun prune() {
        try {
            val failed: List<String> = pruner.prune().failedDeletes
            if (failed.isNotEmpty()) {
                logger.warn("backup prune could not delete ${failed.size} object(s): $failed")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(
                "backup prune failed after a verified upload: ${e::class.simpleName}. " +
                    "The snapshot is stored and recorded; only retention was skipped.",
                e,
            )
        }
    }

    private companion object {
        val TRIGGER_RETRY_DELAY = 5.seconds
        const val OWNER_CHANGED = "Snapshot backup finished for an account that is no longer signed in"
        const val NO_SESSION = "Snapshot backup requested with no session; the request was discarded"
    }
}

private enum class SnapshotOutcome { Recorded, NotDue, OwnerChanged, DestinationUndisclosed }

private fun Exception.asDomainException(): DomainException = this as? DomainException ?: DomainException.Unknown(this)

private fun alreadyBackedUpOn(lastSuccessAt: Long?, now: Instant, timeZone: TimeZone): Boolean =
    lastSuccessAt != null && timeZone.dayOf(Instant.fromEpochMilliseconds(lastSuccessAt)) == timeZone.dayOf(now)

private fun TimeZone.dayOf(instant: Instant): LocalDate = instant.toLocalDateTime(this).date
