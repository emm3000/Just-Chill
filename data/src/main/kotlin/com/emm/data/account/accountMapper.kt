package com.emm.data.account

import com.emm.data.Accounts
import com.emm.domain.account.Account
import com.emm.domain.account.AccountUpsert

fun Accounts.toDomain() = Account(
    accountId = accountId,
    name = name,
)

fun List<Accounts>.toDomain() = map(Accounts::toDomain)

fun Accounts.toAccountUpsert() = AccountUpsert(
    accountId = accountId,
    name = name,
    updatedAt = updatedAt,
    createdAt = createdAt,
)

fun Accounts.toAccountModel() = AccountModel(
    accountId = accountId,
    name = name,
    updatedAt = updatedAt,
    createdAt = createdAt,
    isDeleted = isDeleted,
    userId = "",
)

fun AccountModel.toAccountUpsert() = AccountUpsert(
    accountId = accountId,
    name = name,
    updatedAt = updatedAt,
    createdAt = createdAt,
)
