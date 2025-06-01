package com.emm.domain.account

data class Account(
    val accountId: String,
    val name: String,
    val balance: Double,
) {

    override fun toString(): String = name
}