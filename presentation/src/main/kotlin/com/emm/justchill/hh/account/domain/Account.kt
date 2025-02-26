package com.emm.justchill.hh.account.domain

data class Account(
    val accountId: String,
    val name: String,
    val balance: Double,
    val initialBalance: Double,
    val description: String,
) {

    override fun toString(): String = name
}