package com.emm.justchill.hh.domain.transaction

enum class SyncStatus {

    PENDING_INSERT,
    PENDING_UPDATE,
    PENDING_DELETE,
    SYNCED
}