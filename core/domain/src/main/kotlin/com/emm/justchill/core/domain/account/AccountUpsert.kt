package com.emm.justchill.core.domain.account

import com.emm.justchill.core.domain.shared.AccountId

data class AccountUpsert(val accountId: AccountId, val name: String, val type: AccountType = AccountType.Bank)
