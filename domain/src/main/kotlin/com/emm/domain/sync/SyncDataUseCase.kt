package com.emm.domain.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Triggers a full push+pull sync cycle.
 *
 * A [Mutex] serializes concurrent invocations so overlapping calls (e.g. on-resume +
 * manual trigger) do not produce duplicate pushes or cursor races. The second caller
 * waits for the first to finish; both calls complete without error.
 */
class SyncDataUseCase(private val syncRepository: SyncRepository) {

    private val mutex = Mutex()

    suspend operator fun invoke() = mutex.withLock {
        syncRepository.sync()
    }
}
