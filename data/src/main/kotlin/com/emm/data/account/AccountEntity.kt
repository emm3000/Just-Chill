package com.emm.data.account

data class AccountEntity(
    val accountId: String,
    val name: String,
    val syncState: String,
    val isDeleted: Boolean,
    val updatedAt: Long,
    val createdAt: Long,
)
