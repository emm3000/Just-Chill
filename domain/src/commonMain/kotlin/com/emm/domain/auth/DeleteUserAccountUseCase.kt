package com.emm.domain.auth

import com.emm.domain.shared.backup.BackupMetadataStore
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.logging.DiagnosticsLogger
import com.emm.domain.sync.SyncCursorStore
import com.emm.domain.sync.SyncMutex
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

class DeleteUserAccountUseCase(
    private val authRepository: AuthRepository,
    private val claimLocalDataRepository: ClaimLocalDataRepository,
    private val syncCursorStore: SyncCursorStore,
    private val backupMetadataStore: BackupMetadataStore,
    private val syncMutex: SyncMutex,
    private val logger: DiagnosticsLogger,
) {
    suspend operator fun invoke() = syncMutex.withLock {
        val userId = withStepLogging("session resolve") { resolveAuthenticatedUserId() }

        withStepLogging("remote delete") { authRepository.deleteAccount() }

        // NonCancellable: the remote delete above is irreversible, so a cancellation landing after it
        // must not leave local rows tagged with a userId that no longer exists server-side.
        withContext(NonCancellable) {
            withStepLogging("unclaim") { claimLocalDataRepository.unclaimAll(userId) }

            withStepLogging("cursor clear") { syncCursorStore.clear(userId) }

            withStepLogging("backup metadata clear") { backupMetadataStore.clear(userId) }
        }
    }

    // @Suppress: catches everything so the log names which step broke, then rethrows unchanged.
    // CancellationException is excluded — it is not a failure, and re-wrapping it breaks structured
    // concurrency.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> withStepLogging(step: String, block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        logger.warn("Account deletion failed at step: $step", e)
        throw e
    }

    private suspend fun resolveAuthenticatedUserId(): String {
        val status = try {
            withTimeout(SESSION_RESOLVE_TIMEOUT) {
                authRepository.sessionStatus.first { it !is SessionStatus.Initializing }
            }
        } catch (e: TimeoutCancellationException) {
            throw DomainException.NetworkUnavailable(e)
        }
        return (status as? SessionStatus.Authenticated)?.user?.userId
            ?: throw DomainException.Unauthorized("No authenticated session")
    }

    private companion object {
        val SESSION_RESOLVE_TIMEOUT = 10.seconds
    }
}
