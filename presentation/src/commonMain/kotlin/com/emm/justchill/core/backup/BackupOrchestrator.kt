package com.emm.justchill.core.backup

import com.emm.data.backup.backupSnapshotName
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.shared.backup.BackupMetadataStore
import com.emm.domain.shared.backup.BackupPruner
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.shared.backup.BackupUploader
import com.emm.domain.shared.logging.DiagnosticsLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
) {

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
     * the message, because conflation would let an automatic request overwrite a manual one; the
     * flag is consumed at the start of each cycle instead, so a cycle that swallowed a pending
     * manual tap counts as manual. Same shape, same reason, as `SyncOrchestrator`'s.
     */
    @Volatile
    private var manualRequestPending = false

    /**
     * Ask for a backup. Overlapping calls collapse safely; see [requestChannel].
     *
     * @param manual when true the cycle **skips both the dirty check and the once-a-day cap** — the
     *   user asked for a backup, so one is taken even if the ledger has not moved and one was
     *   already taken today. It still respects the session gate and the single-operation guard:
     *   neither is a policy about when a backup is worth taking, and both are correctness.
     *
     * ADR 009 2c-iv adds the Perfil "Back up now" button that calls this. **Having no production
     * caller before then is expected**: the concurrency guard lives here, so the entry point has to
     * live here too, and a button that reached the pipeline around this class could run a second
     * upload alongside an automatic one.
     */
    fun requestBackup(manual: Boolean = false) {
        if (manual) manualRequestPending = true
        requestChannel.trySend(Unit)
    }

    /**
     * Starts the trigger and the sequential consumer. Call once, after Koin is initialised.
     *
     * Not calling it — which is what [SNAPSHOT_BACKUP_ENABLED] being false does — launches no
     * collector and no consumer, and the consumer is the only caller of the private cycle, so
     * nothing here can reach the network.
     */
    fun start() {
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
     * One cycle: consume the manual flag, and never let anything out.
     *
     * The catch is what keeps the request consumer alive. Without it a single unmapped throwable
     * would break out of the `for` loop draining [requestChannel] and leave the app with no backups
     * at all until the next cold start — a failure that looks exactly like "backup is working" from
     * the outside, which is the shape ADR 009 hard constraint 4 exists to forbid.
     */
    // Intentional broad catch: nothing may escape into externalScope; CancellationException is re-thrown.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun runBackup() {
        val manual = manualRequestPending
        manualRequestPending = false
        val userId = currentUserId ?: return
        try {
            takeSnapshot(userId, manual)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val kind = if (manual) "manual" else "auto"
            logger.warn(
                "backup cycle failed ($kind): ${e::class.simpleName}. " +
                    "The last-successful watermark is untouched, so the next trigger retries.",
                e,
            )
        }
    }

    /**
     * Export, name, upload, record, prune — the order argued in the class KDoc.
     *
     * The clock is read **once**, and the same instant stamps the payload's `exportedAt`, the object
     * name and the recorded watermark. Three reads would let a snapshot be named a second after the
     * data it holds, and the recorded watermark is deliberately the instant the cycle *started*: a
     * write landing during the upload then still reads as dirty next time, which errs toward one
     * extra backup rather than a lost change.
     */
    private suspend fun takeSnapshot(userId: String, manual: Boolean) {
        val takenAt: Instant = clock.now()
        if (!manual && !isBackupDue(userId, takenAt)) return

        val payload: String = backupRepository.exportToJson(
            exportedAt = takenAt.toEpochMilliseconds(),
            appVersion = appVersion,
        )
        // backupSnapshotName is called, never re-implemented: it is public for exactly this caller,
        // and a second spelling of the format would leave the prune unable to recognise the app's
        // own snapshots — silently, since a name it cannot parse is simply not a prune candidate.
        uploader.upload(backupSnapshotName(takenAt), payload)
        metadata.setLastSuccessfulBackupAt(userId, takenAt.toEpochMilliseconds())
        prune()
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
    }
}

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
