package com.emm.data.account

import com.emm.data.Accounts
import com.emm.domain.account.Account
import com.emm.domain.account.AccountType
import com.emm.domain.account.AccountUpsert
import com.emm.domain.shared.AccountId

// SQLDelight -> Entity (internal, stays within data source)
fun Accounts.asEntity() = AccountEntity(
    accountId = accountId,
    name = name,
    type = type,
    updatedAt = updatedAt,
    createdAt = createdAt,
)

fun List<Accounts>.asEntity() = map(Accounts::asEntity)

// Entity -> Domain
fun AccountEntity.asExternalModel() = Account(
    accountId = AccountId(accountId),
    name = name,
    // Accounts coerce to Bank instead of skipping the row: AccountType only drives the list
    // icon (display-only, no financial semantics). Skipping an account row would orphan its
    // transactions in the UI while they still count toward balances, which is worse than
    // showing a generic icon.
    type = runCatching { AccountType.valueOf(type) }.getOrDefault(AccountType.Bank),
)

fun List<AccountEntity>.asExternalModel() = map(AccountEntity::asExternalModel)

// Domain upsert -> Entity
fun AccountUpsert.asEntity() = AccountEntity(
    accountId = accountId.value,
    name = name,
    type = type.name,
    updatedAt = updatedAt,
    createdAt = createdAt,
)
