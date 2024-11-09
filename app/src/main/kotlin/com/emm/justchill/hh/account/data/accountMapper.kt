package com.emm.justchill.hh.account.data

import com.emm.justchill.Accounts
import com.emm.justchill.hh.account.domain.Account

fun Accounts.toDomain() = Account(
    accountId = accountId,
    name = name,
    balance = balance,
    initialBalance = initialBalance,
    description = description.orEmpty(),
)

fun List<Accounts>.toDomain() = map(Accounts::toDomain)

fun Account.toModel(userId: String) = AccountModel(
    accountId = accountId,
    name = name,
    balance = balance,
    initialBalance = initialBalance,
    description = description,
    userId = userId
)