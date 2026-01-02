package com.emm.data.account

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountModel(

    @SerialName("account_id")
    val accountId: String,

    val name: String,

    val balance: Double,

    @SerialName("is_deleted")
    val isDeleted: Boolean,

    @SerialName("updated_at")
    val updatedAt: Long,

    @SerialName("created_at")
    val createdAt: Long,

    @SerialName("user_id")
    val userId: String,
)