package com.emm.domain.sync

interface SyncCursorStore {
    fun lastPulledAt(userId: String): String?
    fun setLastPulledAt(userId: String, cursor: String)

    fun clear(userId: String)
}
