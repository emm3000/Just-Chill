package com.emm.data.account

import com.emm.data.Accounts
import com.emm.domain.account.Account
import com.emm.domain.account.AccountUpsert

// SQLDelight -> Entity (internal, stays within data source)
fun Accounts.asEntity() = AccountEntity(
    accountId = accountId,
    name = name,
    syncState = syncState,
    isDeleted = isDeleted,
    updatedAt = updatedAt,
    createdAt = createdAt,
)

fun List<Accounts>.asEntity() = map(Accounts::asEntity)

// Entity -> Domain
fun AccountEntity.asExternalModel() = Account(
    accountId = accountId,
    name = name,
)

fun List<AccountEntity>.asExternalModel() = map(AccountEntity::asExternalModel)

// Domain upsert -> Entity
fun AccountUpsert.asEntity() = AccountEntity(
    accountId = accountId,
    name = name,
    syncState = "",
    isDeleted = false,
    updatedAt = updatedAt,
    createdAt = createdAt,
)

// Network -> Entity
fun NetworkAccount.asEntity() = AccountEntity(
    accountId = accountId,
    name = name,
    syncState = "",
    isDeleted = isDeleted,
    updatedAt = updatedAt,
    createdAt = createdAt,
)

// Entity -> Network
fun AccountEntity.asNetworkModel(userId: String) = NetworkAccount(
    accountId = accountId,
    name = name,
    isDeleted = isDeleted,
    updatedAt = updatedAt,
    createdAt = createdAt,
    userId = userId,
)
