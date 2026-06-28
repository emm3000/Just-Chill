package com.emm.justchill.core.sync

import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.domain.sync.ObservePendingSyncCountUseCase
import com.emm.domain.sync.SyncDataUseCase
import com.emm.justchill.core.preferences.AppPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * iOS [SyncController] — the automatic-sync core, mirroring `:androidApp`'s SyncOrchestrator. As of
 * slice 6c it carries the SAME four triggers Android has (manual + the three automatic ones); only the
 * connectivity-regained trigger is absent — and Android lacks it too, so that stays shared cross-platform
 * debt, not an iOS gap.
 *
 *  1) Manual: [requestSync] from Perfil's "sincronizar ahora".
 *  2) Sign-in: a sync fires on every distinct authenticated userId transition (covers sign-in-after-claim).
 *  3) On-resume: a sync fires on every app foreground while [SessionStatus.Authenticated], driven by
 *     [resumeEvents] (the iOS analogue of Android's ProcessLifecycleOwner `ON_RESUME`). Mirrors Android
 *     trigger (a): `observeSession().flatMapLatest { if authenticated → resumeEvents else flowOf() }`.
 *  4) Debounced writes: while authenticated, observes [ObservePendingSyncCountUseCase]; debounces bursts
 *     by 3 s and fires only when count > 0 to avoid a feedback loop (sync flips rows to Synced, count → 0).
 *     Mirrors Android trigger (c) verbatim, including the 3-second window.
 *
 * Concurrency: a [Channel.CONFLATED] channel serializes requests. Overlapping triggers collapse into
 * at most one queued run, and [SyncStatus.isSyncing] reflects a single in-flight cycle.
 *
 * Failure posture: [DomainException.Unauthorized] → sign out (the remote session is gone). All other
 * [DomainException] subtypes set [SyncStatus.lastSyncFailed] so Perfil can show the retry pill; the
 * next [requestSync] is the retry. [CancellationException] is never caught.
 *
 * `last_synced_at_$userId` (Long, epoch millis, sentinel -1L = never) is persisted via the shared
 * [AppPreferences] — the SAME accessor Android's SyncOrchestrator uses — so both platforms route this
 * timestamp through one abstraction. It is NOT part of the [com.emm.domain.sync.SyncCursorStore]
 * interface; account deletion still wipes it through [AppPreferences.clearSyncMetadata] (called by
 * DefaultSyncCursorStore.clear).
 *
 * All four trigger loops are launched from [init] on [appScope], so simply binding this single in Koin
 * starts them. A bound-but-not-started orchestrator would make [requestSync] a silent no-op.
 *
 * @param syncData            serialized push+pull use case (holds the shared SyncMutex).
 * @param observeSession      session-status flow from the auth port.
 * @param observePendingCount pending-row count across all tables; gates the debounced-writes trigger.
 * @param signOut             sign-out use case; called only on [DomainException.Unauthorized].
 * @param prefs               key-value preferences adapter; persists last-synced-at per user.
 * @param appScope            application-lifetime [CoroutineScope]; owns the launched jobs.
 * @param resumeEvents        emits [Unit] on every foreground (iOS `UIApplicationDidBecomeActive`); the
 *                            platform-injected analogue of Android's `resumeEvents` (ON_RESUME).
 */
@OptIn(FlowPreview::class)
class IosSyncOrchestrator(
    private val syncData: SyncDataUseCase,
    private val observeSession: ObserveSessionUseCase,
    private val observePendingCount: ObservePendingSyncCountUseCase,
    private val signOut: SignOutUseCase,
    private val prefs: AppPreferences,
    private val appScope: CoroutineScope,
    private val resumeEvents: Flow<Unit>,
) : SyncController {

    private val _status = MutableStateFlow(SyncStatus())
    override val status: StateFlow<SyncStatus> = _status.asStateFlow()

    // CONFLATED: overlapping requests collapse; only one extra run queues behind the active cycle.
    private val requestChannel = Channel<Unit>(Channel.CONFLATED)

    // Tracks the currently authenticated userId so runSync can persist last-synced-at per user.
    @Volatile
    private var currentUserId: String? = null

    init {
        startConsumer()
        startSignInTrigger()
        startResumeTrigger()
        startDebounceTrigger()
    }

    override fun requestSync(manual: Boolean) {
        // No `manual` event channel on iOS: ProfileViewModel surfaces failures via status.lastSyncFailed
        // (the RetryPill), not via one-shot SyncEvents. The flag is accepted for interface parity.
        requestChannel.trySend(Unit)
    }

    /** Consumer: runs queued sync requests one at a time, in order. */
    private fun startConsumer() {
        appScope.launch {
            for (ignored in requestChannel) {
                runSync()
            }
        }
    }

    /**
     * Sign-in trigger: on every distinct authenticated userId transition, load the stored
     * last-synced-at into the UI and request a sync. Mirrors SyncOrchestrator.kt trigger (b).
     */
    private fun startSignInTrigger() {
        appScope.launch {
            observeSession()
                .distinctUntilChangedBy { status ->
                    when (status) {
                        is SessionStatus.Authenticated -> status.user.userId
                        else -> null
                    }
                }
                .collect { sessionStatus ->
                    when (sessionStatus) {
                        is SessionStatus.Authenticated -> {
                            val userId = sessionStatus.user.userId
                            currentUserId = userId
                            val stored = prefs.lastSyncedAt(userId)
                            _status.value = SyncStatus(isSyncing = false, lastSyncedAtMillis = stored)
                            requestSync()
                        }

                        SessionStatus.NotAuthenticated -> {
                            currentUserId = null
                            _status.value = SyncStatus()
                        }

                        SessionStatus.Initializing -> {
                            // Still resolving — leave state unchanged.
                        }
                    }
                }
        }
    }

    /**
     * On-resume trigger: fires a sync on every app foreground while authenticated. Mirrors
     * SyncOrchestrator.kt trigger (a) exactly — `flatMapLatest` on the session gates the resume
     * collector, so foregrounds while signed out emit nothing (the empty [flowOf]) and the collector
     * is torn down the instant the session leaves Authenticated.
     */
    private fun startResumeTrigger() {
        appScope.launch {
            observeSession()
                .flatMapLatest { sessionStatus ->
                    if (sessionStatus is SessionStatus.Authenticated) {
                        resumeEvents
                    } else {
                        flowOf() // no-op: emit nothing when not authenticated
                    }
                }
                .collect { requestSync() }
        }
    }

    /**
     * Debounced-writes trigger: while authenticated, observes the pending-row count, debounces bursts
     * by 3 s, and requests a sync only when count > 0. Mirrors SyncOrchestrator.kt trigger (c) verbatim,
     * including the 3-second window. The `count > 0` filter prevents a feedback loop: sync flips rows
     * Pending→Synced, emitting count 0, which must not retrigger another sync.
     */
    private fun startDebounceTrigger() {
        appScope.launch {
            observeSession()
                .flatMapLatest { sessionStatus ->
                    if (sessionStatus is SessionStatus.Authenticated) {
                        observePendingCount()
                    } else {
                        flowOf(0L)
                    }
                }
                .filter { count -> count > 0L }
                .debounce(3.seconds)
                .collect { requestSync() }
        }
    }

    /**
     * Executes one push+pull cycle. Sets [SyncStatus.isSyncing] for the duration, persists
     * last-synced-at on success, signs out on [DomainException.Unauthorized], and flags
     * [SyncStatus.lastSyncFailed] on any other [DomainException].
     */
    // Intentional broad catch: DomainException subtypes handled explicitly; CancellationException re-thrown.
    // SwallowedException: the inner signOut catch is intentional best-effort — the session is already gone.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun runSync() {
        _status.value = _status.value.copy(isSyncing = true, lastSyncFailed = false)
        try {
            syncData()
            val now = Clock.System.now().toEpochMilliseconds()
            val userId = currentUserId
            if (userId != null) {
                prefs.setLastSyncedAt(userId, now)
            }
            _status.value = _status.value.copy(isSyncing = false, lastSyncedAtMillis = now, lastSyncFailed = false)
        } catch (e: CancellationException) {
            // Must not be swallowed — propagate to the coroutine machinery.
            throw e
        } catch (e: DomainException.Unauthorized) {
            _status.value = _status.value.copy(isSyncing = false)
            try {
                signOut()
            } catch (_: Exception) {
                // Best-effort sign-out; session is already revoked remotely.
            }
        } catch (e: DomainException) {
            // Silent-retry posture: the next requestSync is the retry. lastSyncFailed lets Perfil
            // show the error state regardless of whether the failure was manual or sign-in triggered.
            _status.value = _status.value.copy(isSyncing = false, lastSyncFailed = true)
        }
    }
}
