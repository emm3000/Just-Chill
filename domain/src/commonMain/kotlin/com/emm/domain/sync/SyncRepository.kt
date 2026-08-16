package com.emm.domain.sync

import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    suspend fun sync()

    fun observePendingCount(): Flow<Long>
}
