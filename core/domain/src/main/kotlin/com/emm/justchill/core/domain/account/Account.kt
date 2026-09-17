package com.emm.justchill.core.domain.account

import com.emm.justchill.core.domain.shared.AccountId

data class Account(val accountId: AccountId, val name: String, val type: AccountType = AccountType.Bank) {

    override fun toString(): String = name

    companion object {

        val Empty = Account(
            accountId = AccountId(""),
            name = "",
        )
    }
}
