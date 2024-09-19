package com.emm.justchill.hh.domain.account

data class Account(
    val accountId: String,
    val name: String,
    val balance: Double,
    val description: String,
    val syncStatus: String,
)