package com.emm.justchill.core.database.account

data class AccountEntity(
    val accountId: String,
    val name: String,
    val type: String,
    val updatedAt: Long,
    val createdAt: Long,
)
