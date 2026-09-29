package com.emm.justchill.core.domain.shared.backup

interface SnapshotStore {

    suspend fun export(): LocalSnapshot

    suspend fun restore(snapshot: LocalSnapshot): ImportStats

    suspend fun latestLocalChangeAt(): Long?
}
