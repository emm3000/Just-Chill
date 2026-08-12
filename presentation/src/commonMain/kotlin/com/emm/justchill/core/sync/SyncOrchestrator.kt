package com.emm.justchill.core.sync

import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.domain.sync.SyncDataUseCase
import com.emm.domain.sync.SyncLogger
import com.emm.domain.sync.SyncRepository
import com.emm.justchill.core.preferences.AppPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

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
 * All other [DomainException] subtypes are swallowed silently for automatic triggers — the next
 * trigger is the retry. Manually-requested cycles (see [requestSync]) emit [SyncEvent.SyncFailed].
 * [CancellationException] is never caught. [events] is buffered, so an event raised before the UI
 * exists — the very first cycle runs from `bootstrapAppGraph`, ahead of any composition — waits for
 * its collector instead of being dropped.
 *
 * Sync is a background convenience, never a reason to lose the app. Nothing here is allowed to
 * escape into [externalScope], because that scope carries no handler and an escaped throwable is
 * process death for a local-first app that works perfectly well offline:
 *  - every trigger runs inside [launchResilientTrigger], which absorbs a failed flow chain (e.g. a
 *    SQLite error mid-observe) and re-subscribes after [TRIGGER_RETRY_DELAY];
 *  - [runSync] catches non-[DomainException] throwables as a last resort, so one unmapped failure
 *    cannot end the sequential consumer for the rest of the process lifetime.
 * Every swallow above is reported through [logger] — this class must degrade quietly for the user,
 * not for whoever has to diagnose it.
 *
 * @param syncData            serialized push+pull use case.
 * @param observeSession      session-status flow from auth port.
 * @param syncRepository      source of the pending-row count across all tables.
 * @param signOut             sign-out use case; called only on Unauthorized.
 * @param prefs               SharedPreferences adapter; persists last-synced-at per user.
 * @param externalScope       application-lifetime [CoroutineScope]; owns all launched jobs.
 * @param resumeEvents        emits [Unit] on every foreground ON_RESUME; injected so tests can fake it.
 * @param logger              makes the swallowed failures above observable.
 */
// LongParameterList: eight collaborators, each a distinct port this class orchestrates. Grouping
// them into a holder would only move the list somewhere with no behaviour of its own.
@Suppress("LongParameterList")
@OptIn(FlowPreview::class)
class SyncOrchestrator(
    private val syncData: SyncDataUseCase,
    private val observeSession: ObserveSessionUseCase,
    private val syncRepository: SyncRepository,
    private val signOut: SignOutUseCase,
    private val prefs: AppPreferences,
    private val externalScope: CoroutineScope,
    private val resumeEvents: Flow<Unit>,
    private val logger: SyncLogger,
) : SyncController {

    private val _status = MutableStateFlow(SyncStatus())
    override val status: StateFlow<SyncStatus> = _status.asStateFlow()

    // Channel, not MutableSharedFlow: the first sync cycle runs from bootstrapAppGraph in
    // Application.onCreate, before any composition exists to collect. A shared flow with no
    // subscribers drops what it emits, so a SessionExpired raised in that window was lost for good.
    // A buffered channel holds the event until SyncEventsHandler attaches. replay is deliberately
    // NOT the fix — it would re-deliver stale one-shot events on every re-subscription, re-showing
    // the snackbar after a config change. Same trade-off, same shape as MviViewModel's effects.
    private val _events = Channel<SyncEvent>(Channel.BUFFERED)
    override val events: Flow<SyncEvent> = _events.receiveAsFlow()

    // CONFLATED: overlapping requests collapse; only one extra run queues behind the active cycle.
    private val requestChannel = Channel<Unit>(Channel.CONFLATED)

    // Tracks the currently authenticated userId so runSync can persist last-synced-at per user.
    @Volatile
    private var currentUserId: String? = null

    /**
     * Sticky flag set when a manual sync is requested. Cannot travel through [Channel.CONFLATED]
     * because conflation might let an automatic [Unit] overwrite a manual one. Instead, the flag
     * is consumed at the start of each [runSync] cycle — so any cycle that includes a pending
     * manual tap counts as manual and will surface failures via [SyncEvent.SyncFailed].
     *
     * Note: if a manual request collapses with an automatic one (CONFLATED), the merged cycle
     * still counts as manual because the flag was set before conflation occurred.
     */
    @Volatile
    private var manualRequestPending = false

    /**
     * Offer a sync request. Overlapping calls collapse safely.
     *
     * @param manual when true, marks the cycle as manually requested. A failure in a manual cycle
     *               emits [SyncEvent.SyncFailed] rather than being swallowed silently. The flag is
     *               sticky across CONFLATED collapsing: if a manual request collapses with an
     *               automatic one, the merged cycle counts as manual.
     */
    override fun requestSync(manual: Boolean) {
        if (manual) manualRequestPending = true
        requestChannel.trySend(Unit)
    }

    /**
     * Starts the three trigger listeners and the sequential sync consumer.
     * Call once from Application.onCreate after Koin is initialised.
     */
    fun start() {
        // Consumer: runs sync requests one at a time in sequential order. Not wrapped in
        // launchResilientTrigger — runSync swallows everything itself, so this loop only ends
        // when the scope is cancelled.
        externalScope.launch {
            for (ignored in requestChannel) {
                runSync()
            }
        }

        // Trigger (a): on-resume → requestSync while authenticated.
        // Uses flatMapLatest on session: when not authenticated the resume collector is cancelled.
        launchResilientTrigger("resume") {
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
        launchResilientTrigger("session") {
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
        launchResilientTrigger("writes") {
            observeSession().flatMapLatest { sessionStatus ->
                if (sessionStatus is SessionStatus.Authenticated) {
                    syncRepository.observePendingCount()
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
     * Launches one trigger and keeps it alive across failures.
     *
     * The flows the triggers collect are database- and auth-provider-backed, so any of them can
     * throw mid-observe (a full disk, a corrupt page, a provider error). Collected bare, that
     * throwable leaves the collector, reaches [externalScope] — which has no handler by
     * construction — and takes the process down over a feature the user may not even use. Catching
     * it here and re-subscribing after [TRIGGER_RETRY_DELAY] degrades the trigger instead: sync
     * pauses, the app does not. The delay is what stops a permanently-broken source from becoming
     * a retry storm.
     *
     * [CancellationException] is rethrown first so scope cancellation still tears the trigger down.
     */
    // Intentional broad catch: this is the backstop that keeps a trigger failure off the app scope.
    @Suppress("TooGenericExceptionCaught")
    private fun launchResilientTrigger(name: String, block: suspend () -> Unit) {
        externalScope.launch {
            while (isActive) {
                try {
                    block()
                    // All three trigger sources are endless by construction (StateFlow /
                    // callbackFlow / SQLDelight asFlow), so returning normally means one of them
                    // was swapped for a flow that completes. Looping again would spin the CPU with
                    // no delay, so stop — loudly, because the trigger is now gone.
                    logger.warn("sync trigger '$name' completed unexpectedly; not restarting")
                    return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.warn("sync trigger '$name' failed; restarting in $TRIGGER_RETRY_DELAY", e)
                    delay(TRIGGER_RETRY_DELAY)
                }
            }
        }
    }

    /**
     * Executes one push+pull cycle.
     *
     * Sets [isSyncing] to true for the duration, persists last-synced-at on success,
     * handles [DomainException.Unauthorized] by signing out and emitting [SyncEvent.SessionExpired],
     * silently swallows other [DomainException] variants for automatic triggers (next trigger retries),
     * and emits [SyncEvent.SyncFailed] when the cycle was manually requested.
     *
     * Anything that is not a [DomainException] is caught by the last-resort branch and treated the
     * same way. It should be unreachable — [com.emm.domain.sync.SyncDataUseCase] maps its failures —
     * but an unmapped throwable escaping here would break out of the `for` loop that drains the
     * request channel and leave the app with no sync at all until the next cold start.
     */
    // Intentional broad catch: DomainException subtypes handled explicitly; CancellationException re-thrown.
    // SwallowedException: the Unauthorized branch reacts to the TYPE, not the instance — sign out and
    // emit SessionExpired. Every other catch here logs the throwable it swallows.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun runSync() {
        val manual = manualRequestPending
        manualRequestPending = false
        val cycleKind = if (manual) "manual" else "auto"
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
            } catch (signOutError: Exception) {
                // Best-effort sign-out; session is already revoked remotely. Logged rather than
                // swallowed outright: a sign-out that keeps failing leaves stale local credentials.
                logger.warn("best-effort signOut failed: ${signOutError::class.simpleName}", signOutError)
            }
            _events.send(SyncEvent.SessionExpired)
        } catch (e: DomainException) {
            // Silent-retry posture for automatic triggers: swallow, the next trigger will retry.
            // lastSyncFailed is always set so the Perfil row can show the error state regardless
            // of whether the failure was manual or automatic.
            logger.warn("sync cycle failed ($cycleKind): ${e::class.simpleName}", e)
            _status.value = _status.value.copy(isSyncing = false, lastSyncFailed = true)
            if (manual) _events.send(SyncEvent.SyncFailed(e))
        } catch (e: Exception) {
            // Last resort: keep the consumer loop alive. Same user-facing posture as the branch
            // above, wrapped in Unknown so a manual tap still gets a translated message.
            logger.warn("sync cycle failed (unmapped, $cycleKind): ${e::class.simpleName}", e)
            _status.value = _status.value.copy(isSyncing = false, lastSyncFailed = true)
            if (manual) _events.send(SyncEvent.SyncFailed(DomainException.Unknown(e)))
        }
    }

    private companion object {
        /**
         * Back-off before re-subscribing a failed trigger. Long enough that a persistent failure
         * (a full disk) costs a handful of retries per minute instead of a spin.
         */
        val TRIGGER_RETRY_DELAY = 5.seconds
    }
}
