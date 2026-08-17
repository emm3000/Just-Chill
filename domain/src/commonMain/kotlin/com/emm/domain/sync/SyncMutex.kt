package com.emm.domain.sync

import com.emm.domain.shared.error.DomainException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// Must be wired as a singleton — one lock per process.
// Account deletion, sync and the backup upload share it: work finishing after the delete_account RPC
// re-writes what the server just wiped, and the stateless JWT still satisfies the RLS uid claim.
class SyncMutex {

    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T {
        acquire()
        return try {
            block()
        } finally {
            mutex.unlock()
        }
    }

    // Only the wait is bounded; the block runs unbounded on purpose. Account deletion is irreversible
    // once its delete_account RPC returns, so a timeout able to cancel it mid-flight would trade a
    // hang for local rows tagged with a userId the server no longer has.
    // The failure is a DomainException, never the TimeoutCancellationException withTimeout throws:
    // BackupOrchestrator re-throws every CancellationException into the scope draining its request
    // channel, so a cancellation escaping here would kill backups for the rest of the process.
    private suspend fun acquire() {
        try {
            withTimeout(ACQUIRE_TIMEOUT) { mutex.lock() }
        } catch (e: TimeoutCancellationException) {
            throw DomainException.Busy("Another operation held the shared lock for over $ACQUIRE_TIMEOUT", e)
        }
    }

    private companion object {
        // Covers the whole account deletion (10s session resolve + the 10s Postgrest RPC + local
        // clears) and stops well short of the 120s Storage transferTimeout a single stalled upload
        // call is allowed — waiting that out behind a delete button is the defect, not the fix.
        val ACQUIRE_TIMEOUT: Duration = 30.seconds
    }
}
