package com.emm.domain.sync

import kotlinx.coroutines.flow.Flow

/**
 * Thin delegate: emits the total count of locally-pending rows across all sync tables.
 *
 * Consumers should filter on `count > 0` and apply a debounce before triggering a sync
 * cycle to avoid feedback loops (sync itself flips rows Pending → Synced, emitting 0).
 */
class ObservePendingSyncCountUseCase(private val syncRepository: SyncRepository) {
    operator fun invoke(): Flow<Long> = syncRepository.observePendingCount()
}
