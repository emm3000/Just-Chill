package com.emm.domain.account

import com.emm.domain.shared.currentTimeInMillis

data class AccountUpsert(
    val accountId: String,
    val name: String,
    val balance: Double,
    val updatedAt: Long = currentTimeInMillis(),
    val createdAt: Long = currentTimeInMillis(),
)