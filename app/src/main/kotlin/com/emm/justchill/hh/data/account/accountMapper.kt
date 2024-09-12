package com.emm.justchill.hh.data.account

import com.emm.justchill.Accounts
import com.emm.justchill.hh.domain.account.Account

fun Accounts.toDomain() = Account(
    accountId = accountId,
    name = name,
    balance = balance,
    description = description.orEmpty(),
    syncStatus = syncStatus
)

fun List<Accounts>.toDomain() = map(Accounts::toDomain)

fun Account.toModel(userId: String) = AccountModel(
    accountId = accountId,
    name = name,
    balance = balance,
    description = description,
    userId = userId
)