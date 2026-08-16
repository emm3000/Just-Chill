package com.emm.domain.sync

class SyncDataUseCase(private val syncRepository: SyncRepository, private val syncMutex: SyncMutex) {

    suspend operator fun invoke() = syncMutex.withLock {
        syncRepository.sync()
    }
}
