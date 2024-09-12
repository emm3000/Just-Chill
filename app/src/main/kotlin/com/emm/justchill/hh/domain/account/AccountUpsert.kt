package com.emm.justchill.hh.domain.account

data class AccountUpsert(
    val name: String,
    val balance: Double,
    val description: String,
)