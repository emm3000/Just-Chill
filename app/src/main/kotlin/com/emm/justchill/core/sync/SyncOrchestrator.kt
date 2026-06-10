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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Current sync status exposed to the UI.
 *
 * @property isSyncing true while a sync cycle is in progress.
 * @property lastSyncedAtMillis epoch-millis of the last successful sync for the current user,
 *           or null when the user has never synced or is not signed in.
 */
data class SyncStatus(val isSyncing: Boolean = false, val lastSyncedAtMillis: Long? = null)

/** One-shot events emitted by [SyncOrchestrator] that require top-level UI handling. */
sealed interface SyncEvent {
    /** The remote session was revoked — the user has been signed out automatically. */
    data object SessionExpired : SyncEvent
}

/**
 * Orchestrates automatic foreground sync (no WorkManager, no realtime).
 *
 * Triggers:
 *  a) On-resume: fires a sync on every app foreground while the user is [SessionStatus.Authenticated].
 *  b) Sign-in: fires a sync on every distinct authenticated userId transition (covers sign-in-after-claim).
 *  c) Debounced writes: while authenticated, observes pending-count; debounces bursts by 3 s and
 *     fires only when count > 0 to avoid a feedback loop (sync flips rows to Synced, count → 0).
 *
 * Concurrency: a [Channel.CONFLATED] channel serializes requests. Overlapping triggers collapse
 * into at most one queued run, and [isSyncing] accurately reflects a single in-flight cycle.
 *
 * Failure posture: [DomainException.Unauthorized] → sign out + emit [SyncEvent.SessionExpired].
 * All other [DomainException] subtypes are swallowed silently — the next trigger is the retry.
 * [CancellationException] is never caught.
 *
 * @param syncData            serialized push+pull use case.
 * @param observeSession      session-status flow from auth port.
 * @param observePendingCount pending-row count across all tables.
 * @param signOut             sign-out use case; called only on Unauthorized.
 * @param prefs               SharedPreferences adapter; persists last-synced-at per user.
 * @param externalScope       application-lifetime [CoroutineScope]; owns all launched jobs.
 * @param resumeEvents        emits [Unit] on every foreground ON_RESUME; injected so tests can fake it.
 */
@OptIn(FlowPreview::class)
class SyncOrchestrator(
    private val syncData: SyncDataUseCase,
    private val observeSession: ObserveSessionUseCase,
    private val observePendingCount: ObservePendingSyncCountUseCase,
    private val signOut: SignOutUseCase,
    private val prefs: AppPreferences,
    private val externalScope: CoroutineScope,
    private val resumeEvents: Flow<Unit>,
) {

    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val _events = MutableSharedFlow<SyncEvent>()
    val events: SharedFlow<SyncEvent> = _events.asSharedFlow()

    // CONFLATED: overlapping requests collapse; only one extra run queues behind the active cycle.
    private val requestChannel = Channel<Unit>(Channel.CONFLATED)

    // Tracks the currently authenticated userId so runSync can persist last-synced-at per user.
    @Volatile
    private var currentUserId: String? = null

    /** Offer a sync request. Overlapping calls collapse safely. */
    fun requestSync() {
        requestChannel.trySend(Unit)
    }

    /**
     * Starts the three trigger listeners and the sequential sync consumer.
     * Call once from Application.onCreate after Koin is initialised.
     */
    fun start() {
        // Consumer: runs sync requests one at a time in sequential order.
        externalScope.launch {
            for (ignored in requestChannel) {
                runSync()
            }
        }

        // Trigger (a): on-resume → requestSync while authenticated.
        // Uses flatMapLatest on session: when not authenticated the resume collector is cancelled.
        externalScope.launch {
            observeSession().flatMapLatest { sessionStatus ->
                if (sessionStatus is SessionStatus.Authenticated) {
                    resumeEvents
                } else {
                    flowOf() // no-op: emit nothing when not authenticated
                }
            }.collect { requestSync() }
        }

        // Trigger (b): sign-in → requestSync on distinct userId transition to Authenticated.
        // Also updates currentUserId and loads the stored last-synced-at for the UI.
        externalScope.launch {
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

        // Trigger (c): debounced local writes → requestSync only when count > 0 and authenticated.
        // The filter `count > 0` prevents a feedback loop: sync flips rows Pending→Synced,
        // emitting count 0, which must not retrigger another sync.
        externalScope.launch {
            observeSession().flatMapLatest { sessionStatus ->
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
     * Executes one push+pull cycle.
     *
     * Sets [isSyncing] to true for the duration, persists last-synced-at on success,
     * handles [DomainException.Unauthorized] by signing out and emitting [SyncEvent.SessionExpired],
     * and silently swallows other [DomainException] variants (next trigger retries).
     */
    // Intentional broad catch: DomainException subtypes handled explicitly; CancellationException re-thrown.
    // SwallowedException: the inner signOut catch is intentional best-effort — the session is already gone.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun runSync() {
        _status.value = _status.value.copy(isSyncing = true)
        try {
            syncData()
            val now = System.currentTimeMillis()
            val userId = currentUserId
            if (userId != null) {
                prefs.setLastSyncedAt(userId, now)
            }
            _status.value = _status.value.copy(isSyncing = false, lastSyncedAtMillis = now)
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
            _events.emit(SyncEvent.SessionExpired)
        } catch (_: DomainException) {
            // Silent-retry posture: swallow, the next trigger will retry.
            _status.value = _status.value.copy(isSyncing = false)
        }
    }
}
