package com.emm.domain.account

import com.emm.domain.shared.AccountId

/**
 * What the user is asking to create or rename.
 *
 * `createdAt` and `updatedAt` are absent for the same reason they are absent from
 * `TransactionInsert`: they are storage metadata stamped by `:data` at the write, not something a
 * caller supplies. As `= currentTimeInMillis()` defaults they made this data class read the wall
 * clock on construction, and they were ignored on the update path anyway — `AccountLocalDataSource
 * .update` always stamped its own.
 */
data class AccountUpsert(val accountId: AccountId, val name: String, val type: AccountType = AccountType.Bank)
