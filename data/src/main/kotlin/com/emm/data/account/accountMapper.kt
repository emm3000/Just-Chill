package com.emm.data.account

import com.emm.data.Accounts
import com.emm.domain.account.Account
import com.emm.domain.account.AccountType
import com.emm.domain.account.AccountUpsert
import com.emm.domain.account.Currency
import com.emm.domain.shared.AccountId

// SQLDelight -> Entity (internal, stays within data source)
fun Accounts.asEntity() = AccountEntity(
    accountId = accountId,
    name = name,
    type = type,
    currency = currency,
    syncState = syncState,
    isDeleted = isDeleted,
    updatedAt = updatedAt,
    createdAt = createdAt,
)

fun List<Accounts>.asEntity() = map(Accounts::asEntity)

// Entity -> Domain
fun AccountEntity.asExternalModel() = Account(
    accountId = AccountId(accountId),
    name = name,
    type = runCatching { AccountType.valueOf(type) }.getOrDefault(AccountType.Bank),
    currency = runCatching { Currency.valueOf(currency) }.getOrDefault(Currency.ARS),
)

fun List<AccountEntity>.asExternalModel() = map(AccountEntity::asExternalModel)

// Domain upsert -> Entity
fun AccountUpsert.asEntity() = AccountEntity(
    accountId = accountId.value,
    name = name,
    type = type.name,
    currency = currency.name,
    syncState = "",
    isDeleted = false,
    updatedAt = updatedAt,
    createdAt = createdAt,
)

// Network -> Entity
fun NetworkAccount.asEntity() = AccountEntity(
    accountId = accountId,
    name = name,
    type = type,
    currency = currency,
    syncState = "",
    isDeleted = isDeleted,
    updatedAt = updatedAt,
    createdAt = createdAt,
)

// Entity -> Network
fun AccountEntity.asNetworkModel(userId: String) = NetworkAccount(
    accountId = accountId,
    name = name,
    type = type,
    currency = currency,
    isDeleted = isDeleted,
    updatedAt = updatedAt,
    createdAt = createdAt,
    userId = userId,
)
