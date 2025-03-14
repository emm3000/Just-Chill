package com.emm.domain.account

data class Account(
    val accountId: String,
    val name: String,
    val balance: Double,
    val description: String,
    val isSelected: AccountSelect,
) {

    override fun toString(): String = name
}