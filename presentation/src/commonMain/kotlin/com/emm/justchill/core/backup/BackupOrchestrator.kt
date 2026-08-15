package com.emm.justchill.core.backup

import com.emm.data.backup.backupSnapshotName
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.shared.backup.BackupMetadataStore
import com.emm.domain.shared.backup.BackupPruner
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.shared.backup.BackupUploader
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

/**
 * Decides when the device takes a snapshot backup, and takes exactly one at a time.
 *
 * ADR 009 (`docs/sync/ADR009_PLAN.md`, phase 2c) built every piece this class calls and left nothing
 * calling them: the export, the verified upload, the retention prune and the two watermarks all
 * existed with zero callers. This is the caller. It owns the *decision* and the *concurrency*, and
 * no step of the pipeline itself — a step that fails belongs to whichever port it came from.
 *
 * It is also the [BackupController] the shared UI consumes: [isBackingUp] while a cycle runs, and
 * one [BackupEvent] per **manually requested** cycle. Both are argued on the port, not here.
 *
 * ### The decision, in order
 *
 *  1. **Authenticated only.** The trigger flows are collected inside a `flatMapLatest` over
 *     [ObserveSessionUseCase], so the collectors are cancelled the moment the session ends rather
 *     than merely producing requests nobody serves. A session is also what gives the upload a
 *     destination: `docs/adr/009` decision 5 keys a device's snapshots on the account it is signed
 *     into, so signing out is the mechanism for ceasing to upload to one.
 *  2. **Dirty.** [BackupRepository.latestLocalChangeAt] against
 *     [BackupMetadataStore.lastSuccessfulBackupAt]. Dirty means the watermark exists — a `null` is
 *     four empty tables, not an old change — and either nothing was ever backed up or the watermark
 *     is newer than what was.
 *  3. **At most one automatic snapshot per calendar day**, and **the cap counts successes, not
 *     attempts**: it compares the day of the last *recorded* success against today, so a failed
 *     upload records nothing and the next trigger tries again instead of the failure burning the
 *     day. Calendar day is resolved in the injected [timeZone] — "is it still the same day" is a
 *     calendar question and answering it with millisecond arithmetic gets the boundary wrong for
 *     every user not sitting on UTC (`docs/DATE_AUDIT.md` rule 7).
 *
 * ### The order inside a successful cycle is load-bearing
 *
 * Export → name → upload → **record** → prune. The recording sits between the upload and the prune
 * on purpose, and both neighbours matter:
 *
 *  - **An upload failure must leave the watermark untouched**, which is what makes the daily cap a
 *    cap on successes. It throws out of [BackupUploader.upload] before the write, so this is
 *    structural rather than remembered.
 *  - **The account is checked at both ends of the cycle, not only at the start.** The capture at the
 *    top is a decision; the upload and the record each verify it still holds, because a cycle
 *    suspends for up to two minutes and the session collector runs elsewhere. Both halves are argued
 *    on [takeSnapshot].
 *  - **A prune failure must not propagate or undo the success.** The snapshot is uploaded and
 *    verified by then; retention is housekeeping over objects that are already safe, and the next
 *    prune sees the same leftovers. Letting it throw would mark a good backup as a failed cycle and
 *    re-run the whole upload on the next trigger.
 *
 * Every swallow above is reported through [logger] — ADR 009 hard constraint 4 is that no failure in
 * this pipeline is silent, and the 2026-08-12 outage was invisible precisely because one path
 * logged nothing.
 *
 * ### Nothing here may reach the application scope
 *
 * [externalScope] carries no handler by construction, so an escaped throwable is process death over
 * a feature the app is fully usable without. Both defences are copied from `SyncOrchestrator` rather
 * than shared with it, because Phase 5 deletes that class: the trigger runs inside
 * [launchResilientTrigger], which absorbs a failed flow chain and re-subscribes, and [runBackup]
 * catches everything else so one bad cycle cannot end the request consumer for the rest of the
 * process lifetime.
 *
 * @param backupRepository produces the snapshot and answers the dirty question.
 * @param uploader         stores it and proves it arrived intact; returning normally IS the proof.
 * @param pruner           retention, run after a success and never allowed to fail the cycle.
 * @param metadata         the per-user last-successful-backup watermark this class writes.
 * @param observeSession   session-status flow; the gate and the source of the user id.
 * @param appVersion       stamped into the export payload; a qualified String from the platform module.
 * @param clock            read ONCE per cycle, for the payload stamp, the name and the watermark.
 * @param timeZone         the zone the once-a-day cap is a calendar question in.
 * @param externalScope    application-lifetime scope; owns every job this class launches.
 * @param backgroundEvents app-went-to-background; the primary trigger, injected so tests can fake it.
 * @param resumeEvents     app-came-to-foreground; the staleness retry for a process that died mid-upload.
 * @param logger           makes the swallows above observable.
 */
// LongParameterList: twelve collaborators, each a distinct port or seam this class orchestrates —
// same justification as SyncOrchestrator's. Grouping them into a holder would only move the list
// somewhere with no behaviour of its own, and the two lifecycle flows have to stay separate
// parameters because faking them independently is what the trigger tests are made of.
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

    // No replay, one slot of buffer: an outcome raised with nobody listening is dropped rather than
    // held for the next collector. Argued on BackupController.events — it is the opposite of
    // SyncOrchestrator's buffered channel, and deliberately so.
    private val _events = MutableSharedFlow<BackupEvent>(replay = 0, extraBufferCapacity = 1)
    override val events: Flow<BackupEvent> = _events.asSharedFlow()

    // CONFLATED: the concurrent-operation guard. Overlapping triggers collapse into at most one
    // queued run behind the active one, and a single consumer drains it — so two uploads can never
    // be in flight against the same bucket at once, which is the state ADR 009's naming and
    // read-back verification both assume cannot happen.
    private val requestChannel = Channel<Unit>(Channel.CONFLATED)

    // Tracked from the session gate below so a cycle knows whose watermark it is reading and
    // writing. Null means "no session", and a cycle that finds it null does nothing at all.
    @Volatile
    private var currentUserId: String? = null

    /**
     * Sticky flag set by a manual request. It cannot travel through [Channel.CONFLATED] as part of
     * the message, because conflation would let an automatic request overwrite a manual one — this
     * flag is what lets a manual tap survive being conflated with an already-queued automatic
     * trigger. [runBackup] consumes it with a plain read then a plain write, not an atomic
     * read-modify-write, against the plain write [requestBackup] performs from Main.
     *
     * **The two can interleave, and the window is real, not just theoretical.** A manual tap landing
     * between [runBackup]'s read and its write is swallowed rather than counted: the read already
     * captured `false`, so the *running* cycle proceeds as automatic and reports nothing on
     * [events]; the write that follows then clears the flag the tap just set, so the request already
     * sitting in [requestChannel] — the very one the tap's own `trySend` queued — drains as
     * automatic too. In that nanosecond window the user taps "Respaldar ahora" and gets no answer at
     * all. Same shape, same window, inherited from `SyncOrchestrator`'s. Left as-is on purpose: it is
     * a handful of nanoseconds inside a suspend function's prologue, not a case worth an atomic or a
     * mutex for a feature this class already treats as best-effort.
     */
    @Volatile
    private var manualRequestPending = false

    /**
     * Set by [start], and never anywhere else. [requestBackup] reads it because
     * [SNAPSHOT_BACKUP_ENABLED] being false means [start] is never called: nothing then drains
     * [requestChannel], so a `trySend` would land on a channel with no consumer — forever, with no
     * upload, no failure, no event and, before this flag, no log either. This is the second half of
     * that gate: not a replacement for [SNAPSHOT_BACKUP_ENABLED], which stays the one deciding
     * whether the button renders at all, but a backstop for every other way `requestBackup` could be
     * reached — the automatic triggers, a future caller, a test that forgets to call [start].
     */
    @Volatile
    private var started = false

    /**
     * Ask for a backup. Overlapping calls collapse safely; see [requestChannel].
     *
     * A call before [start] has run is dropped rather than queued, and [logger] says so — see
     * [started]. That is the only new outcome; everything below still describes what happens once
     * the orchestrator is running.
     *
     * @param manual when true the cycle **skips both the dirty check and the once-a-day cap** — the
     *   user asked for a backup, so one is taken even if the ledger has not moved and one was
     *   already taken today. It still respects the session gate and the single-operation guard:
     *   neither is a policy about when a backup is worth taking, and both are correctness.
     *
     * **A manual request found with no session never becomes a cycle** — no export, upload, record
     * or prune runs. The flag is consumed at the top of [runBackup], *before* the session gate
     * returns, so a tap while signed out clears it and a later sign-in does not inherit a backup
     * nobody is waiting for any more. That is the choice and not an oversight: the alternative fires
     * a snapshot at some arbitrary later moment, long after the screen that asked for it is gone, and
     * a "manual" backup the user cannot connect to anything they did is worse than none.
     *
     * **It does, however, publish [BackupEvent.Failed] on [events].** An earlier version of this
     * class discarded that request in total silence, on the reasoning that `ProfileViewModel
     * .backUpNow` already refuses a signed-out tap at the button and so this branch could never fire
     * in production — true only of the session *at tap time*. `backUpNow` reads `currentState
     * .session` and then calls this method; [runBackup] reads [currentUserId] whenever the single
     * consumer gets around to draining the request, which can be later. A session that ends in that
     * window leaves the tap answered by nobody — no upload, no failure, no event, and with no
     * suspension point between raising and lowering [isBackingUp] the `StateFlow` may conflate the
     * flicker away too — so this is now the one place left that can answer it. `SyncOrchestrator`
     * cannot show the difference: `runSync` has no auth gate at all.
     *
     * The Perfil "Respaldar ahora" row (ADR 009 2c-iv) is the production caller, and it comes through
     * here rather than around it: the concurrency guard lives in this class, so a button reaching the
     * pipeline any other way could run a second upload alongside an automatic one.
     */
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

    /**
     * Starts the trigger and the sequential consumer. Call once, after Koin is initialised.
     *
     * Sets [started] first and synchronously, before either coroutine below is launched: a call to
     * [requestBackup] racing this method sees either "not started yet" (dropped and logged) or
     * "started" (queued into [requestChannel], which buffers a `CONFLATED` slot even before the
     * consumer coroutine has had a chance to actually run) — never a request silently lost between
     * the two.
     *
     * Not calling this — which is what [SNAPSHOT_BACKUP_ENABLED] being false does — launches no
     * collector and no consumer, and the consumer is the only caller of the private cycle, so nothing
     * here can reach the network. [requestBackup] now says so instead of staying quiet about it.
     */
    fun start() {
        started = true

        // Consumer: one cycle at a time, in order. Not wrapped in launchResilientTrigger — runBackup
        // swallows everything itself, so this loop only ends when the scope is cancelled.
        externalScope.launch {
            for (ignored in requestChannel) {
                runBackup()
            }
        }

        // Both triggers, one gate. flatMapLatest on the session cancels the merged collector when
        // the session ends, and re-subscribes with the new user id when another begins.
        //
        // A resume arriving right after a background event costs nothing: it runs the same decision,
        // and the daily cap and the dirty check have both already said no. What it buys is the case
        // the plan's Trigger row is written around — a process killed mid-upload leaves the
        // watermark unwritten, so the ledger is still dirty and the next foreground retries.
        launchResilientTrigger("lifecycle") {
            observeSession().flatMapLatest { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        currentUserId = status.user.userId
                        merge(backgroundEvents, resumeEvents)
                    }

                    else -> {
                        currentUserId = null
                        flowOf()
                    }
                }
            }.collect { requestBackup() }
        }
    }

    /**
     * Launches the trigger and keeps it alive across failures.
     *
     * The flows it collects are auth-provider- and platform-lifecycle-backed, so either can throw
     * mid-observe. Collected bare, that throwable leaves the collector, reaches [externalScope] —
     * which has no handler by construction — and takes the process down over a backup the user may
     * never have switched on. Catching it here and re-subscribing after [TRIGGER_RETRY_DELAY]
     * degrades the trigger instead: backups pause, the app does not. The delay is what stops a
     * permanently-broken source from becoming a retry storm.
     *
     * Copied from `SyncOrchestrator.launchResilientTrigger` rather than shared with it: that class
     * and its whole package are deleted by ADR 009 Phase 5, and this one has to outlive it.
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
                    // The session flow is endless by construction (a StateFlow), so returning
                    // normally means the source was swapped for one that completes. Looping again
                    // would spin the CPU with no delay, so stop — loudly, because the trigger is
                    // now gone and nothing else would say so.
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

    /**
     * One cycle: consume the manual flag, publish what happened, and never let anything out.
     *
     * The catch is what keeps the request consumer alive. Without it a single unmapped throwable
     * would break out of the `for` loop draining [requestChannel] and leave the app with no backups
     * at all until the next cold start — a failure that looks exactly like "backup is working" from
     * the outside, which is the shape ADR 009 hard constraint 4 exists to forbid.
     *
     * [isBackingUp] wraps the whole body, session gate included, so it means exactly "the consumer is
     * busy with a request" — including the discarded no-session one. An automatic trigger flips it
     * true and false with no event; a manual one flips it and publishes [BackupEvent.Failed], for the
     * reason argued in [requestBackup]. Anything narrower would have to be kept in step with the
     * early returns below, and a status flag that lies by omission is worse than one that occasionally
     * says "busy" about a cycle that turned out to have nothing to do.
     */
    // Intentional broad catch: nothing may escape into externalScope; CancellationException is re-thrown.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun runBackup() {
        val manual = manualRequestPending
        manualRequestPending = false
        _isBackingUp.value = true
        try {
            val userId = currentUserId
            if (userId == null) {
                // A manual request reaching here with no session means the session ended between
                // ProfileViewModel's tap-time check and this consumer draining the request — see
                // requestBackup's KDoc for why nobody else can answer that tap. An automatic trigger
                // in the same spot has no requester waiting on it and stays silent, same as report().
                if (manual) _events.tryEmit(BackupEvent.Failed(DomainException.Unauthorized(NO_SESSION)))
                return
            }
            report(manual, takeSnapshot(userId, manual))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val kind = if (manual) "manual" else "auto"
            logger.warn(
                "backup cycle failed ($kind): ${e::class.simpleName}. " +
                    "The last-successful watermark is untouched, so the next trigger retries.",
                e,
            )
            if (manual) _events.tryEmit(BackupEvent.Failed(e.asDomainException()))
        } finally {
            _isBackingUp.value = false
        }
    }

    /**
     * Turns one cycle's ending into the answer a tap gets, and stays silent for every other trigger.
     *
     * [SnapshotOutcome.NotDue] deliberately produces nothing rather than a success. It is
     * unreachable for a manual cycle — `manual` skips the dirty check and the daily cap — but if it
     * ever became reachable, reporting "done" for a snapshot that was never taken is precisely the
     * checkmark-over-a-non-event ADR 009 hard constraint 4 exists to forbid.
     */
    private fun report(manual: Boolean, outcome: SnapshotOutcome) {
        if (!manual) return
        val event: BackupEvent? = when (outcome) {
            SnapshotOutcome.Recorded -> BackupEvent.Succeeded
            SnapshotOutcome.OwnerChanged -> BackupEvent.Failed(DomainException.Unauthorized(OWNER_CHANGED))
            SnapshotOutcome.NotDue -> null
        }
        if (event != null) _events.tryEmit(event)
    }

    /**
     * Export, name, upload, record, prune — the order argued in the class KDoc.
     *
     * The clock is read **once**, and the same instant stamps the payload's `exportedAt`, the object
     * name and the recorded watermark. Three reads would let a snapshot be named a second after the
     * data it holds, and the recorded watermark is deliberately the instant the cycle *started*: a
     * write landing during the upload then still reads as dirty next time, which errs toward one
     * extra backup rather than a lost change.
     *
     * ### [userId] is carried through, and checked again at the end
     *
     * A cycle captures the account once and then suspends for a long time — an export plus four
     * round trips under a 120s `transferTimeout` — while the session collector that maintains
     * [currentUserId] runs in a different coroutine and the request consumer is not cancelled on
     * sign-out. So the account can change *inside* a cycle, and both ends of the cycle have to say so
     * rather than assume it cannot:
     *
     *  - **The upload asserts it.** [BackupUploader.upload] takes the account and refuses if the live
     *    session is no longer that one, with a named failure. Without it the destination prefix is a
     *    second, later read of the session than the dirty check and the watermark were made against,
     *    and the bucket's RLS accepts the write because the key matches whoever is signed in *now* —
     *    A's ledger under B's prefix, and A recorded as backed up.
     *  - **The record re-checks it**, because the assertion above only covers up to the last byte
     *    sent, not what happens between `upload` returning and the watermark being written. A normal
     *    return already means the prefix was asserted and RLS accepted every write under it, so a
     *    mismatch here cannot mean the snapshot went somewhere else — it means the account signed out,
     *    or into a different one, in that window between the upload finishing and the record running.
     *    The re-check deliberately does not lean on the upload's assertion still holding by the time it
     *    runs: it reads [currentUserId] fresh rather than trusting that nothing changed since. On a
     *    mismatch nothing is written and the prune is skipped too — a *false* watermark is worse than a
     *    missing one, since it suppresses the next backup for a whole day, while a missing one only
     *    costs the retry the next trigger already provides.
     *
     * The [SnapshotOutcome] it returns exists so [report] can tell those two non-events apart. A
     * `Boolean` could not: "no snapshot" covers both a cycle that had nothing to do and one whose
     * account vanished under it, and only the second is something a tap should hear about.
     */
    private suspend fun takeSnapshot(userId: String, manual: Boolean): SnapshotOutcome {
        val takenAt: Instant = clock.now()
        if (!manual && !isBackupDue(userId, takenAt)) return SnapshotOutcome.NotDue

        val payload: String = backupRepository.exportToJson(
            exportedAt = takenAt.toEpochMilliseconds(),
            appVersion = appVersion,
        )
        // backupSnapshotName is called, never re-implemented: it is public for exactly this caller,
        // and a second spelling of the format would leave the prune unable to recognise the app's
        // own snapshots — silently, since a name it cannot parse is simply not a prune candidate.
        uploader.upload(userId, backupSnapshotName(takenAt), payload)
        // Read once and branched on twice: two reads of a @Volatile field can disagree, and the
        // second one deciding the return value while the first decided the write is how a cycle
        // reports an ending it did not have.
        val stillTheSameAccount: Boolean = currentUserId == userId
        if (stillTheSameAccount) {
            metadata.setLastSuccessfulBackupAt(userId, takenAt.toEpochMilliseconds())
            prune()
        } else {
            logger.warn(
                "backup upload finished for an account that is no longer signed in; the watermark " +
                    "is NOT recorded and retention was skipped. Recording it would mark $userId " +
                    "backed up for the day on the strength of a snapshot this device can no longer " +
                    "see, and suppress the real one.",
            )
        }
        return if (stillTheSameAccount) SnapshotOutcome.Recorded else SnapshotOutcome.OwnerChanged
    }

    /** Dirty first, then the daily cap — and `&&` is what keeps that order real, not just written. */
    private suspend fun isBackupDue(userId: String, now: Instant): Boolean {
        val lastSuccessAt: Long? = metadata.lastSuccessfulBackupAt(userId)
        return isDirty(backupRepository.latestLocalChangeAt(), lastSuccessAt) &&
            !alreadyBackedUpOn(lastSuccessAt, now, timeZone)
    }

    /**
     * Retention, and it can fail without failing the backup.
     *
     * An individual delete that fails does not throw at all — [BackupPruner] reports those in
     * [com.emm.domain.shared.backup.BackupPruneReport.failedDeletes] precisely because one stuck
     * object must not abort the run — so that channel is logged too. A stuck object is a leftover
     * the next prune sees again; the same object stuck for a month is the thing worth noticing, and
     * a report nobody reads cannot distinguish the two.
     *
     * **This catch is not redundant with [runBackup]'s, and the difference is the message.** The
     * prune is the last statement of a cycle whose watermark is already written, so as *control
     * flow* the two are interchangeable — delete this one and every behavioural assertion in
     * `BackupOrchestratorTest` still passes. What changes is what the log says: the outer catch
     * reports "the last-successful watermark is untouched, so the next trigger retries", which for a
     * prune failure is simply **false** — the watermark was written, the snapshot is safe, and the
     * next trigger will not retry anything because the day is spent. Sending whoever is holding an
     * outage to look for a failed upload that succeeded is hard constraint 4's failure mode wearing a
     * log line, so `a failed prune leaves the watermark recorded and throws nothing` asserts this
     * message and asserts the outer one is absent. Delete the catch and that test goes red.
     */
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
        /**
         * Back-off before re-subscribing a failed trigger. Long enough that a persistent failure
         * costs a handful of retries per minute instead of a spin.
         */
        val TRIGGER_RETRY_DELAY = 5.seconds

        /**
         * Diagnostic text for the account-switch refusal, English like every other [DomainException]
         * message. It never reaches the user: `ProfileViewModel` answers a failed backup with its own
         * Spanish copy rather than `toUserMessage()`, which would render this as "sesión expirada".
         */
        const val OWNER_CHANGED = "Snapshot backup finished for an account that is no longer signed in"

        /**
         * Diagnostic text for a manual request that found no session by the time the single consumer
         * drained it — English like every other [DomainException] message, and never reaches the
         * user: `ProfileViewModel` answers every [BackupEvent.Failed] with the same Spanish copy.
         */
        const val NO_SESSION = "Snapshot backup requested with no session; the request was discarded"
    }
}

/** What one cycle actually did. [BackupOrchestrator.report] answers a manual request out of it. */
private enum class SnapshotOutcome { Recorded, NotDue, OwnerChanged }

/**
 * The pipeline's ports throw [DomainException], and the last-resort catch above exists precisely for
 * the throwable that did not — so an unmapped one is wrapped rather than dropped from the report.
 * Same posture, same wrapper, as `SyncOrchestrator`'s unmapped branch.
 */
private fun Exception.asDomainException(): DomainException = this as? DomainException ?: DomainException.Unknown(this)

/**
 * Has anything changed locally that the last recorded backup does not already hold?
 *
 * A `null` watermark is the four backed-up tables holding no row at all — structurally distinct from
 * `0`, because SQLDelight types the column `Long?` — and an empty ledger is nothing to back up. A
 * `null` [lastSuccessAt] is a device that has never completed one, which is dirty as soon as it
 * holds anything.
 */
private fun isDirty(watermarkAt: Long?, lastSuccessAt: Long?): Boolean =
    watermarkAt != null && (lastSuccessAt == null || watermarkAt > lastSuccessAt)

/**
 * Did the last recorded success fall on the same calendar day as [now], in [timeZone]?
 *
 * Two instants six hours apart are the same day in one zone and two days in another, so the answer
 * genuinely depends on the zone and the zone is genuinely injected — no default, per
 * `docs/DATE_AUDIT.md` rule 7, whose whole point is that a parameter defaulting to the ambient zone
 * lets a caller read the machine without saying so.
 */
private fun alreadyBackedUpOn(lastSuccessAt: Long?, now: Instant, timeZone: TimeZone): Boolean =
    lastSuccessAt != null && timeZone.dayOf(Instant.fromEpochMilliseconds(lastSuccessAt)) == timeZone.dayOf(now)

private fun TimeZone.dayOf(instant: Instant): LocalDate = instant.toLocalDateTime(this).date
