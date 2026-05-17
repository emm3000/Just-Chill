package com.emm.domain.account

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.currentTimeInMillis

data class AccountUpsert(
    val accountId: AccountId,
    val name: String,
    val type: AccountType = AccountType.Bank,
    val currency: Currency = Currency.PEN,
    val updatedAt: Long = currentTimeInMillis(),
    val createdAt: Long = currentTimeInMillis(),
)
