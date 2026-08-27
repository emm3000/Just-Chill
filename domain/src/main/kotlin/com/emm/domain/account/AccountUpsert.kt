package com.emm.domain.account

import com.emm.domain.shared.AccountId

data class AccountUpsert(val accountId: AccountId, val name: String, val type: AccountType = AccountType.Bank)
