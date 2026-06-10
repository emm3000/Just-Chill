package com.emm.domain.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Application-wide lock serializing every operation that touches remote sync state.
 *
 * Held by [SyncDataUseCase] for full push+pull cycles and by
 * [com.emm.domain.auth.DeleteUserAccountUseCase] for account deletion. Sharing one lock closes
 * a race where an in-flight push could re-upsert rows AFTER the server-side `delete_account`
 * RPC wiped them: the JWT is stateless (stays valid until expiry even though the auth user is
 * gone), the remote schema has no FK to auth.users by design, and RLS passes because the uid
 * claim still matches — resurrected rows would be unreadable by anyone, forever, breaking the
 * deletion promise. With the shared lock, deletion waits for any in-flight sync, and a sync
 * queued behind a deletion no-ops because the session is already cleared.
 *
 * Must be wired as a singleton — one lock per process.
 */
class SyncMutex {

    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }
}
