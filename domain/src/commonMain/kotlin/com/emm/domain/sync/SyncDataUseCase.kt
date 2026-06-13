package com.emm.domain.sync

/**
 * Triggers a full push+pull sync cycle.
 *
 * The shared [SyncMutex] serializes concurrent invocations so overlapping calls (e.g. on-resume +
 * manual trigger) do not produce duplicate pushes or cursor races, and also serializes sync
 * against account deletion (see [SyncMutex]). The second caller waits for the first to finish;
 * both calls complete without error.
 */
class SyncDataUseCase(private val syncRepository: SyncRepository, private val syncMutex: SyncMutex) {

    suspend operator fun invoke() = syncMutex.withLock {
        syncRepository.sync()
    }
}
