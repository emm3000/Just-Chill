package com.emm.domain.account

import com.emm.domain.shared.SyncState

data class AccountUpsert(
    val accountId: String,
    val name: String,
    val balance: Double,
    val updatedAt: Long,
    val createdAt: Long,
    val syncState: SyncState,
)