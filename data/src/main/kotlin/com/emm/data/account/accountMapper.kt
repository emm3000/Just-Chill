package com.emm.data.account

import com.emm.data.Accounts
import com.emm.domain.account.Account

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