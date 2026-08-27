package com.emm.domain.shared

import com.emm.domain.shared.error.DomainException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// Must be wired as a singleton — one lock per process.
// Account deletion and the backup upload share it: work finishing after the delete_account RPC
// re-writes what the server just wiped, and the stateless JWT still satisfies the RLS uid claim.
class RemoteWriteMutex(private val acquireTimeout: Duration = ACQUIRE_TIMEOUT) {

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
    private suspend fun acquire() {
        // withTimeout cancels concurrently with its own block, so the cancellation can land after
        // mutex.lock() already took the lock — every exit below has to give it back.
        var acquired = false
        try {
            withTimeout(acquireTimeout) {
                mutex.lock()
                acquired = true
            }
        } catch (e: TimeoutCancellationException) {
            if (acquired) mutex.unlock()
            // The failure is a DomainException, never the TimeoutCancellationException itself:
            // BackupOrchestrator re-throws every CancellationException into the scope draining its
            // request channel, so a cancellation escaping here would kill backups for the process.
            throw DomainException.Busy("Another operation held the shared lock for over $acquireTimeout", e)
        } catch (e: CancellationException) {
            if (acquired) mutex.unlock()
            throw e
        }
    }

    private companion object {
        // Covers an ordinary account deletion (10s session resolve, the 10s Postgrest RPC, local
        // clears) and stops well short of the 120s Storage transferTimeout a single stalled upload
        // call is allowed. It does NOT cover the worst case — requireValidSession can force a 10s
        // token refresh ahead of the RPC — and the waiter then fails Busy and retries, which is the
        // designed give-up rather than a longer wait behind a delete button.
        val ACQUIRE_TIMEOUT: Duration = 30.seconds
    }
}
