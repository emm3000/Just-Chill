package com.emm.domain.sync

import com.emm.domain.shared.error.DomainException
import kotlinx.coroutines.flow.Flow

/**
 * Synchronises local data with the remote backend: push pending rows, then pull remote
 * changes using the server-set cursor (ADR 002).
 *
 * @throws DomainException on failure.
 */
interface SyncRepository {
    suspend fun sync()

    /**
     * Emits the total count of locally-pending rows across all tables (accounts, categories,
     * transactions, recurring_movements) that have syncState = 'Pending' and a non-null userId.
     *
     * Used by [ObservePendingSyncCountUseCase] to trigger a debounced sync after local writes.
     * Emits 0 when every row is synced or no user is signed in.
     */
    fun observePendingCount(): Flow<Long>
}
