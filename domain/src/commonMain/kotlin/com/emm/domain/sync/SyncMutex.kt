package com.emm.domain.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Must be wired as a singleton — one lock per process.
// Sync and account deletion share it: an in-flight push finishing after the delete_account RPC
// re-upserts rows the server just wiped, and the stateless JWT still satisfies the RLS uid claim.
class SyncMutex {

    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }
}
