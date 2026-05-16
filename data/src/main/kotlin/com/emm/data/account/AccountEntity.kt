package com.emm.data.account

data class AccountEntity(
    val accountId: String,
    val name: String,
    val type: String,
    val currency: String,
    val updatedAt: Long,
    val createdAt: Long,
)
