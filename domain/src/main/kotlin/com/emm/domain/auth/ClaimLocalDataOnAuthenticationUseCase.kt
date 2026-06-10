package com.emm.domain.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull

/**
 * Long-running coordinator: whenever the session becomes [SessionStatus.Authenticated], claims all
 * anonymous-local rows (userId IS NULL) for that user.
 *
 * Why an observer instead of claiming inline in [SignInUseCase] / [SignUpUseCase]:
 * - Decoupling: a claim failure never breaks the sign-in/sign-up UX — the user is signed in cleanly.
 * - Self-healing: [ClaimLocalDataUseCase] is idempotent (guards on userId IS NULL), so a transient
 *   failure is retried on the next Authenticated emission (app restart, token refresh).
 * - Completeness: rows created while offline get claimed on the next Authenticated emission, not only
 *   at the instant of sign-in.
 *
 * Collect this from an application-scoped coroutine; it runs for the lifetime of the process.
 */
class ClaimLocalDataOnAuthenticationUseCase(
    private val observeSession: ObserveSessionUseCase,
    private val claimLocalData: ClaimLocalDataUseCase,
) {

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    suspend operator fun invoke() {
        observeSession()
            .mapNotNull { (it as? SessionStatus.Authenticated)?.user?.userId }
            .distinctUntilChanged()
            .collect { userId ->
                try {
                    claimLocalData(userId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Swallowed on purpose: claimAll is idempotent, so the next Authenticated
                    // emission retries. A transient claim failure must not tear down this observer.
                }
            }
    }
}
