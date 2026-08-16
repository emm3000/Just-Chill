package com.emm.domain.auth

import com.emm.domain.shared.logging.DiagnosticsLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlin.time.Duration.Companion.seconds

class ClaimLocalDataOnAuthenticationUseCase(
    private val observeSession: ObserveSessionUseCase,
    private val claimLocalData: ClaimLocalDataUseCase,
    private val claimLocalDataRepository: ClaimLocalDataRepository,
    private val logger: DiagnosticsLogger,
) {

    // Never completes: collect it from an application-scoped coroutine.
    // @Suppress: a failed claim is swallowed on purpose — claimAll is idempotent, so the next
    // emission retries — and logged so the swallow stays observable.
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
                    logger.warn("claim failed: ${e::class.simpleName}", e)
                }
            }
    }

    private companion object {
        val RETRY_DELAY = 5.seconds
    }
}
