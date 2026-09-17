package com.emm.domain.auth

import com.emm.domain.shared.RemoteWriteMutex
import com.emm.domain.shared.backup.BackupEraser
import com.emm.domain.shared.backup.BackupMetadataStore
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.logging.DiagnosticsLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

// Step order is fixed: sweep, then metadata clear, then the RPC. `deleteAccount()` kills the
// session, so a sweep or clear placed after it throws `Unauthorized`; a clear before an aborted
// sweep drops `destinationDisclosedAt` and leaves backups off with nothing actually deleted.
class DeleteUserAccountUseCase(
    private val authRepository: AuthRepository,
    private val backupEraser: BackupEraser,
    private val backupMetadataStore: BackupMetadataStore,
    private val remoteWriteMutex: RemoteWriteMutex,
    private val logger: DiagnosticsLogger,
) {
    suspend operator fun invoke() = remoteWriteMutex.withLock {
        val userId = withStepLogging("session resolve") { resolveAuthenticatedUserId() }

        withStepLogging("backup erase") { backupEraser.eraseOwnedBackups(userId) }

        withStepLogging("backup metadata clear") { backupMetadataStore.clear(userId) }

        withStepLogging("remote delete") { authRepository.deleteAccount() }
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
