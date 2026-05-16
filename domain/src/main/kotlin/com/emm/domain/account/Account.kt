package com.emm.domain.account

data class Account(
    val accountId: String,
    val name: String,
    val type: AccountType = AccountType.Bank,
    val currency: Currency = Currency.ARS,
) {

    override fun toString(): String = name

    companion object {

        val Empty = Account(
            accountId = "",
            name = "",
        )
    }
}
