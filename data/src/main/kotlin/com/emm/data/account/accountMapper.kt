package com.emm.data.account

import com.emm.data.Accounts
import com.emm.domain.account.Account
import com.emm.domain.account.AccountUpsert
import com.emm.domain.shared.SyncState

fun Accounts.toDomain() = Account(
    accountId = accountId,
    name = name,
    balance = balance,
)

fun List<Accounts>.toDomain() = map(Accounts::toDomain)

fun Accounts.toAccountUpsert() = AccountUpsert(
    accountId = accountId,
    name = name,
    balance = balance,
    updatedAt = updatedAt,
    createdAt = createdAt,
    syncState = SyncState.Synced,
)

fun Accounts.toAccountModel() = AccountModel(
    accountId = accountId,
    name = name,
    balance = balance,
    updatedAt = updatedAt,
    createdAt = createdAt,
)

fun AccountModel.toAccountUpsert() = AccountUpsert(
    accountId = accountId,
    name = name,
    balance = balance,
    updatedAt = updatedAt,
    createdAt = createdAt,
    syncState = SyncState.Synced,
)