package com.emm.domain.account

data class AccountUpsert(
    val name: String,
    val balance: Double,
    val description: String,
)