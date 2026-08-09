package com.emm.domain.auth

import com.emm.domain.sync.SyncLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlin.time.Duration.Companion.seconds

/**
 * Long-running coordinator: whenever the session is [SessionStatus.Authenticated] AND there are
 * anonymous-local rows (userId IS NULL), claims those rows for the authenticated user immediately.
 *
 * Why reactive (observeUnclaimedCount) instead of triggering only on userId transition:
 * - Rows created WHILE already signed in get syncState='Pending' but userId=NULL (the insert
 *   queries never set userId; only claimAll does). The old distinctUntilChanged approach emitted
 *   only once per userId transition, so a new unclaimed row created mid-session produced no new
 *   session emission and was never claimed until cold start.
 * - This approach flatMaps on session status and, while Authenticated, reacts to EVERY increase in
 *   unclaimed-row count — no transition needed, no stale userId gap.
 *
 * Why an observer instead of claiming inline in [SignInUseCase] / [SignUpUseCase]:
 * - Decoupling: a claim failure never breaks the sign-in/sign-up UX — the user is signed in cleanly.
 * - Self-healing: [ClaimLocalDataUseCase] is idempotent (guards on userId IS NULL), so a transient
 *   failure is retried the next time the unclaimed count rises above 0. A failure of the COUNT
 *   QUERY itself used to break that promise — it terminated the flow, and with it the observer, for
 *   the rest of the process. [retryWhen] re-subscribes after [RETRY_DELAY] so the promise holds for
 *   both failure modes.
 * - Completeness: rows created while offline get claimed on the next Authenticated emission, not only
 *   at the instant of sign-in.
 *
 * Collect this from an application-scoped coroutine; it runs for the lifetime of the process.
 */
class ClaimLocalDataOnAuthenticationUseCase(
    private val observeSession: ObserveSessionUseCase,
    private val claimLocalData: ClaimLocalDataUseCase,
    private val claimLocalDataRepository: ClaimLocalDataRepository,
    private val logger: SyncLogger,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Suppress("TooGenericExceptionCaught")
    suspend operator fun invoke() {
        observeSession()
            .flatMapLatest { status ->
                if (status is SessionStatus.Authenticated) {
                    claimLocalDataRepository.observeUnclaimedCount()
                        .filter { unclaimed -> unclaimed > 0L }
                        .map { status.user.userId }
                } else {
                    emptyFlow()
                }
            }
            // The chain itself can fail (a SQLite error mid-observe on the unclaimed-count query,
            // an auth-provider error on the session flow). Without this the observer would die
            // silently and never claim again until the next cold start. Re-subscribing after a
            // fixed delay is enough: everything downstream is idempotent.
            .retryWhen { cause, _ ->
                if (cause is CancellationException) {
                    false
                } else {
                    logger.warn("claim observer failed; retrying in $RETRY_DELAY", cause)
                    delay(RETRY_DELAY)
                    true
                }
            }
            .collect { userId ->
                try {
                    claimLocalData(userId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Swallowed on purpose: claimAll is idempotent, so the next emission retries.
                    // Logged so the swallow is observable rather than invisible.
                    logger.warn("claim failed: ${e::class.simpleName}", e)
                }
            }
    }

    private companion object {
        /** Back-off before re-subscribing to a failed chain. Long enough not to spin on a hard error. */
        val RETRY_DELAY = 5.seconds
    }
}
