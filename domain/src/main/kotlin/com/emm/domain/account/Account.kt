package com.emm.domain.account

import com.emm.domain.shared.AccountId

data class Account(val accountId: AccountId, val name: String, val type: AccountType = AccountType.Bank) {

    override fun toString(): String = name

    companion object {

        val Empty = Account(
            accountId = AccountId(""),
            name = "",
        )
    }
}
