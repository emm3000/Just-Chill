package com.emm.justchill.hh.account.domain

data class AccountUpsert(
    val name: String,
    val balance: Double,
    val description: String,
)