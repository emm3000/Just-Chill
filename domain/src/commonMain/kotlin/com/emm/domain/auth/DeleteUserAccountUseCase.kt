package com.emm.domain.auth

import com.emm.domain.shared.backup.BackupEraser
import com.emm.domain.shared.backup.BackupMetadataStore
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.logging.DiagnosticsLogger
import com.emm.domain.sync.SyncMutex
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

/**
 * The cloud backup sweep precedes the deletion RPC because it cannot follow it: `delete_account()`
 * runs `delete from auth.users` and `deleteAccount()` clears the local session right after, so
 * `ownedPrefix()` would throw `Unauthorized` for any sweep placed later.
 *
 * The metadata clear cannot follow the RPC either, for the same dead session, and it must not
 * precede the sweep: `clear` also drops `destinationDisclosedAt`, which gates every upload, so a
 * clear before an aborted sweep leaves backups switched off after a deletion that removed nothing,
 * recoverable only by the user re-entering Perfil. It therefore sits in the single gap between the
 * finished sweep and the RPC.
 */
class DeleteUserAccountUseCase(
    private val authRepository: AuthRepository,
    private val backupEraser: BackupEraser,
    private val backupMetadataStore: BackupMetadataStore,
    private val syncMutex: SyncMutex,
    private val logger: DiagnosticsLogger,
) {
    suspend operator fun invoke() = syncMutex.withLock {
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
