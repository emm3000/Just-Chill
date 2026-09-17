package com.emm.justchill.core.database.backup

import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.account.AccountType
import com.emm.justchill.core.domain.shared.AccountId
import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(val accountId: String, val name: String, val type: String, val currency: String = "PEN")

fun Account.toDto() = AccountDto(
    accountId = accountId.value,
    name = name,
    type = type.name,
    currency = "PEN",
)

fun AccountDto.toEntity() = Account(
    accountId = AccountId(accountId),
    name = name,
    type = runCatching { AccountType.valueOf(type) }.getOrDefault(AccountType.Bank),
)
