package com.emm.justchill.hh.transaction.domain

enum class SyncStatus {

    PENDING_INSERT,
    PENDING_UPDATE,
    PENDING_DELETE,
    SYNCED
}