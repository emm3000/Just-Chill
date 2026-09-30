package com.emm.justchill.core.domain.shared.backup

import kotlinx.coroutines.flow.Flow

interface SnapshotStore {

    suspend fun export(): LocalSnapshot

    suspend fun restore(snapshot: LocalSnapshot): ImportStats

    suspend fun latestLocalChangeAt(): Long?

    fun observeLatestLocalChangeAt(): Flow<Long?>
}
