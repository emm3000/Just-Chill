package com.emm.data.account

import com.emm.data.Accounts
import com.emm.domain.account.Account
import com.emm.domain.account.AccountSelect

fun Accounts.toDomain() = Account(
    accountId = accountId,
    name = name,
    balance = balance,
    description = description.orEmpty(),
    isSelected = AccountSelect.valueOf(defaultSelection)
)

fun List<Accounts>.toDomain() = map(Accounts::toDomain)

fun Account.toModel(userId: String) = AccountModel(
    accountId = accountId,
    name = name,
    balance = balance,
    description = description,
    userId = userId
)