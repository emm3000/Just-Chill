package com.emm.domain.transaction

enum class SyncStatus {

    PENDING_INSERT,
    PENDING_UPDATE,
    PENDING_DELETE,
    SYNCED
}