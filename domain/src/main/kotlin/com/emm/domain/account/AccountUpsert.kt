package com.emm.domain.account

import com.emm.domain.shared.currentTimeInMillis

data class AccountUpsert(
    val accountId: String,
    val name: String,
    val type: AccountType = AccountType.Bank,
    val currency: Currency = Currency.ARS,
    val updatedAt: Long = currentTimeInMillis(),
    val createdAt: Long = currentTimeInMillis(),
)
