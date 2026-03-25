package com.emm.domain.account

data class Account(
    val accountId: String,
    val name: String,
) {

    override fun toString(): String = name

    companion object {

        val Empty = Account(
            accountId = "",
            name = "",
        )
    }
}
