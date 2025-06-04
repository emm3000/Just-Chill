package com.emm.domain.account

data class AccountUpsert(
    val accountId: String,
    val name: String,
    val balance: Double,
    val updatedAt: Long,
    val isSynced: Boolean = false,
)