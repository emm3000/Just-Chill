package com.emm.data.account

import com.emm.data.Accounts
import com.emm.domain.account.Account
import com.emm.domain.account.AccountType
import com.emm.domain.shared.AccountId

fun Accounts.asEntity() = AccountEntity(
    accountId = accountId,
    name = name,
    type = type,
    updatedAt = updatedAt,
    createdAt = createdAt,
)

fun List<Accounts>.asEntity() = map(Accounts::asEntity)

fun AccountEntity.asExternalModel() = Account(
    accountId = AccountId(accountId),
    name = name,
    // Accounts coerce to Bank instead of skipping the row: AccountType is display-only, and skipping
    // the row would orphan its transactions in the UI while they still count toward balances.
    type = runCatching { AccountType.valueOf(type) }.getOrDefault(AccountType.Bank),
)

fun List<AccountEntity>.asExternalModel() = map(AccountEntity::asExternalModel)
